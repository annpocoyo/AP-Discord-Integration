package com.annpocoyo.apdiscordintegration;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import com.annpocoyo.apdiscordintegration.minecraft.McInterface;
import com.annpocoyo.apdiscordintegration.minecraft.McInterfaceImpl;
import com.annpocoyo.apdiscordintegration.minecraft.event.McEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.McGameMessageEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.McMessageEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartingEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppingEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.players.McPlayerCountChanged;
import com.annpocoyo.apdiscordintegration.social.SocialInterface;
import com.annpocoyo.apdiscordintegration.social.discord.DiscordInterface;
import com.annpocoyo.apdiscordintegration.social.event.SocialEvent;
import com.annpocoyo.apdiscordintegration.social.event.SocialMessageEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.BridgeDisconnectEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.BridgeReconnectEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.startup.SocialReadyEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.startup.SocialStartFailureEvent;
import com.annpocoyo.apdiscordintegration.util.ChatFormat;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

/**
 * Handles tying Minecraft and Discord together
 */
public class SocialBridge {
    private final static SocialBridge INSTANCE = new SocialBridge();
    public static SocialBridge getInstance() {
        return INSTANCE;
    }

    // State
    private final AtomicBoolean shutDown = new AtomicBoolean(false);

    // Interfaces
    private final McInterface mcInterface = new McInterfaceImpl();
    private final SocialInterface socialInterface = new DiscordInterface();
    // Queues
    private final BlockingQueue<McEvent> mcQueue = new ArrayBlockingQueue<>(512);
    private final BlockingQueue<SocialEvent> socialQueue = new ArrayBlockingQueue<>(512);
    // Executor
    private final ExecutorService mcQueueExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService socialQueueExecutor = Executors.newSingleThreadExecutor();

    public void start() {
        LOGGER.info("Starting bridge...");

        // Load Config
        LOGGER.info("Loading config...");
        ConfigManager.init();

        // Bot Token check
        if (ConfigManager.mainConfig.general.botToken.getDefault().equals(ConfigManager.mainConfig.general.botToken.get())) {
            LOGGER.error("A BOT TOKEN has not been provided! Please edit the config to include the bot token, then restart the server!");
            LOGGER.error("Consult the wiki for more information! The mod will disable itself!");
            return;
        }

        // Start interfaces
        LOGGER.info("Starting interfaces...");
        mcInterface.start(this::enqueueMcEvent);
        socialInterface.start(this::enqueueSocialEvent);

        // Begin processing
        LOGGER.info("Starting queue loop...");
        mcQueueExecutor.submit(() -> loopWrapper(this::mcEventLoop, "MC Loop"));
        socialQueueExecutor.submit(() -> loopWrapper(this::socialEventLoop, "Social Loop"));
    }

    // Loops
    private void mcEventLoop() throws InterruptedException {
        boolean eventHandled = false;

        McEvent currentEvent = mcQueue.take();

        // Events that require Social Bridge
        if (socialInterface.isRunning()) {
            boolean switchTriggered = true;
            switch (currentEvent) {
                case ServerStartingEvent _ ->
                        socialInterface.setStatus(SocialInterface.StatusType.DO_NOT_DISTURB, "Starting...");
                case ServerStartedEvent _ ->{
                    socialInterface.sendServerMessage("Server Started", ChatFormat.GAME);
                    mcInterface.getPlayerCount().thenAccept(this::setPlayerCountStatus);
                }
                case ServerStoppingEvent _ -> {
                    socialInterface.sendServerMessage("Server Stopping...", ChatFormat.GAME, true);
                    socialInterface.setStatus(SocialInterface.StatusType.DO_NOT_DISTURB, "Stopping...");
                }
                case McPlayerCountChanged event -> {
                    if (mcInterface.isRunning())
                        setPlayerCountStatus(event.getNewPlayerCount());
                }
                case McMessageEvent event -> {
                    var message = event.getMessage();
                    var playerName = event.getPlayerName();
                    var type = event.getType();
                    if (playerName.isEmpty())
                        socialInterface.sendServerMessage(message, type);
                    else
                        socialInterface.sendPlayerMessage(message, playerName.orElseThrow(), type, null);
                }
                case McGameMessageEvent event ->
                        socialInterface.sendServerMessage(event.getMessage(), ChatFormat.GAME);
                default ->
                        switchTriggered = false;
            }
            eventHandled = switchTriggered;
        }

        if (currentEvent instanceof ServerStoppedEvent) {
            shutdownBridge();
            eventHandled = true;
        }

        // Log unhandled events
        if (!eventHandled) {
            LOGGER.warn("MC Event not handled: {}", currentEvent);
        }
    }
    private void socialEventLoop() throws InterruptedException {
        boolean eventHandled = false;

        SocialEvent currentEvent = socialQueue.take();

        // Require MCInterface
        if (mcInterface.isRunning()) {
            boolean switchTriggered = true;
            switch (currentEvent) {
                case SocialMessageEvent event ->
                        mcInterface.sendSocialMessage(event.getMessage());
                case BridgeDisconnectEvent _ ->
                        mcInterface.sendSpecialMessage(McInterface.SpecialMessageType.BRIDGE_DISCONNECT);
                case BridgeReconnectEvent _ -> {
                    socialInterface.sendServerMessage("Bridge Reconnected", ChatFormat.GAME);
                    mcInterface.sendSpecialMessage(McInterface.SpecialMessageType.BRIDGE_RECONNECT);
                    mcInterface.getPlayerCount().thenAccept(this::setPlayerCountStatus);
                }
                default ->
                    switchTriggered = false;
            }
            eventHandled = switchTriggered;
        }

        {
            boolean switchTriggered = true;
            switch (currentEvent) {
                case SocialReadyEvent _ -> {
                    if (!mcInterface.isRunning())
                        socialInterface.sendServerMessage("Server Starting...", ChatFormat.GAME, true);
                    else {
                        socialInterface.sendServerMessage("Server Started", ChatFormat.GAME);
                        mcInterface.getPlayerCount().thenAccept(this::setPlayerCountStatus);
                    }
                }
                case SocialStartFailureEvent _ -> shutdownBridge();
                default ->
                        switchTriggered = false;
            }
            eventHandled = switchTriggered || eventHandled;
        }

        // Log unhandled events
        if (!eventHandled) {
            LOGGER.warn("Social Event not handled: {}", currentEvent);
        }
    }

    // Submit to queues
    private boolean enqueueMcEvent(McEvent event) {
        boolean enqueued = mcQueue.offer(event);
        if (!enqueued) {
            LOGGER.warn("An MC Event ({}) has been dropped due to lack of space in queue, is something happening?", event);
        }
        return enqueued;
    }
    private boolean enqueueSocialEvent(SocialEvent event) {
        boolean enqueued = socialQueue.offer(event);
        if (!enqueued) {
            LOGGER.warn("An Social Event ({}) has been dropped due to lack of space in queue, is something happening?", event);
        }
        return enqueued;
    }

    // Shutdown bridge
    private void shutdownBridge() {
        CompletableFuture.runAsync( () -> {
            if (shutDown.compareAndSet(false, true)) {
                LOGGER.info("Shutting down bridge...");
                LOGGER.info("Stopping executors...");
                mcQueueExecutor.shutdownNow();
                socialQueueExecutor.shutdownNow();

                LOGGER.info("Signaling to other components...");
                mcInterface.stop();
                socialInterface.stop("> Server Stopped", true);
            }
        });
    }

    // Util
    private void loopWrapper(InterruptableRunnable loopFunction, String name) {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                loopFunction.run();
            }
        } catch (InterruptedException _) {
            LOGGER.info("Loop Wrapper (wraps: {}) interrupted", name);
        } catch (RuntimeException e) {
            LOGGER.error("An exception occurred in the Discord Bridge, shutting down bridge:", e);
        } finally {
            shutdownBridge();
        }
    }

    @FunctionalInterface
    private interface InterruptableRunnable {
        void run() throws InterruptedException;
    }

    private void setPlayerCountStatus(int playerCount) {
        if (!socialInterface.isRunning())
            return;

        if (playerCount == 0)
            socialInterface.setStatus(SocialInterface.StatusType.IDLE, "No one's online!");
        else
            socialInterface.setStatus(SocialInterface.StatusType.ONLINE, "%s player(s) online!".formatted(playerCount));
    }
}
