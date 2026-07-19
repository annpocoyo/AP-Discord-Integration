package com.annpocoyo.apdiscordintegration;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import com.annpocoyo.apdiscordintegration.minecraft.event.McEvent;
import com.annpocoyo.apdiscordintegration.minecraft.McInterface;
import com.annpocoyo.apdiscordintegration.minecraft.McInterfaceImpl;
import com.annpocoyo.apdiscordintegration.minecraft.event.McGameMessageEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.McMessageEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartingEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppingEvent;
import com.annpocoyo.apdiscordintegration.social.event.SocialMessageEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.SocialReadyEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.SocialStartFailureEvent;
import com.annpocoyo.apdiscordintegration.social.event.SocialEvent;
import com.annpocoyo.apdiscordintegration.social.SocialInterface;
import com.annpocoyo.apdiscordintegration.social.discord.DiscordInterface;
import com.annpocoyo.apdiscordintegration.util.ChatFormat;

import java.util.concurrent.*;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

/**
 * Handles tying Minecraft and Discord together
 */
public class SocialBridge {
    private final static SocialBridge INSTANCE = new SocialBridge();
    public static SocialBridge getInstance() {
        return INSTANCE;
    }

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
        mcQueueExecutor.submit(this::mcEventLoop);
        socialQueueExecutor.submit(this::socialEventLoop);
    }

    // Loops
    private void mcEventLoop() {
        while (true) {
            boolean eventHandled = false;

            McEvent currentEvent;
            try {
                currentEvent = mcQueue.take();
            } catch (InterruptedException e) {
                // Probably asked to stop!
                LOGGER.info("MC Event Loop interrupted!");
                break;
            }

            // Events that require Social Bridge
            if (socialInterface.isRunning()) {
                boolean switchTriggered = true;
                switch (currentEvent) {
                    case ServerStartingEvent _ ->
                            socialInterface.setStatus(SocialInterface.StatusType.DND, "Starting...");
                    case ServerStartedEvent _ ->{
                        socialInterface.sendServerMessage("Server Started", ChatFormat.GAME);
                        socialInterface.setStatus(SocialInterface.StatusType.IDLE);
                    }
                    case ServerStoppingEvent _ -> {
                        socialInterface.sendServerMessage("Server Stopping...", ChatFormat.GAME, true);
                        socialInterface.setStatus(SocialInterface.StatusType.DND, "Stopping...");
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
                LOGGER.warn("MC Event not handled (has the social service been connected to?): {}", currentEvent);
            }
        }
    }
    private void socialEventLoop() {
        while (true) {
            boolean eventHandled = false;

            SocialEvent currentEvent;
            try {
                currentEvent = socialQueue.take();
            } catch (InterruptedException e) {
                // Probably asked to stop!
                LOGGER.info("Social Event Loop interrupted!");
                break;
            }

            // Require MCInterface
            if (mcInterface.isRunning()) {
                boolean switchTriggered = true;
                switch (currentEvent) {
                    case SocialMessageEvent messageEvent ->
                            mcInterface.sendMessage("[DISCORD] <%s> %s ".formatted(messageEvent.getDisplayName(), messageEvent.getMessage()));
                    default ->
                        switchTriggered = false;
                }
                eventHandled = switchTriggered;
            }

            {
                boolean switchTriggered = true;
                switch (currentEvent) {
                    case SocialReadyEvent _ ->
                            socialInterface.sendServerMessage("Server Starting...", ChatFormat.GAME, true);
                    case SocialStartFailureEvent _ -> shutdownBridge();
                    default ->
                            switchTriggered = false;
                }
                eventHandled = switchTriggered || eventHandled;
            }

            // Log unhandled events
            if (!eventHandled) {
                LOGGER.warn("Social Event not handled (has the server finished starting?): {}", currentEvent);
            }
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
        LOGGER.info("Shutting down bridge...");
        LOGGER.info("Stopping executors...");
        mcQueueExecutor.shutdownNow();
        socialQueueExecutor.shutdownNow();

        LOGGER.info("Signaling to other components...");
        mcInterface.stop();
        socialInterface.stop("> Server Stopped", true);
    }
}
