package com.annpocoyo.apdiscordintegration;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import com.annpocoyo.apdiscordintegration.minecraft.event.McEvent;
import com.annpocoyo.apdiscordintegration.minecraft.McInterface;
import com.annpocoyo.apdiscordintegration.minecraft.McInterfaceImpl;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartingEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppingEvent;
import com.annpocoyo.apdiscordintegration.social.event.SocialEvent;
import com.annpocoyo.apdiscordintegration.social.SocialInterface;
import com.annpocoyo.apdiscordintegration.social.discord.DiscordInterface;

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
        mcInterface.start(this::enqueueMcEvent);
        socialInterface.start(this::enqueueSocialEvent);

        // Begin processing
        mcQueueExecutor.submit(this::mcEventLoop);
        socialQueueExecutor.submit(this::socialEventLoop);
    }

    // Loops
    private void mcEventLoop() {
        while (true) {
            McEvent currentEvent;
            try {
                currentEvent = mcQueue.take();
            } catch (InterruptedException e) {
                // Probably asked to stop!
                break;
            }

            // Events that require Social Bridge
            if (!socialInterface.isRunning())
                continue;

            switch (currentEvent) {
                case ServerStartingEvent _ ->
                        socialInterface.sendMessage("Server Starting...");
                case ServerStartedEvent _ ->
                        socialInterface.sendMessage("Server Started");
                case ServerStoppingEvent _ ->
                        socialInterface.sendMessage("Server Stopping...");
                case ServerStoppedEvent _ -> {
                    socialInterface.sendMessage("Server Stopped");
                    shutdownBridge();
                    return;
                }
                default ->
                        LOGGER.warn("Unknown type of MC Event: {}", currentEvent);
            }
        }
    }
    private void socialEventLoop() {
        while (true) {
            SocialEvent currentEvent;
            try {
                currentEvent = socialQueue.take();
            } catch (InterruptedException e) {
                // Probably asked to stop!
                break;
            }

            switch (currentEvent) {
                default ->
                        LOGGER.warn("Unknown type of Social Event: {}", currentEvent);
            }
        }
    }

    // Submit to queues
    private boolean enqueueMcEvent(McEvent event) {
        return mcQueue.offer(event);
    }
    private boolean enqueueSocialEvent(SocialEvent event) {
        return socialQueue.offer(event);
    }

    // Shutdown bridge
    private void shutdownBridge() {
        mcQueueExecutor.shutdownNow();
        socialQueueExecutor.shutdownNow();
        mcInterface.stop();
        socialInterface.stop();
    }
}
