package com.annpocoyo.apdiscordintegration;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import com.annpocoyo.apdiscordintegration.minecraft.McInterface;
import com.annpocoyo.apdiscordintegration.minecraft.McInterfaceImpl;
import com.annpocoyo.apdiscordintegration.social.SocialInterface;
import com.annpocoyo.apdiscordintegration.social.discord.DiscordInterface;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

/**
 * Handles tying Minecraft and Discord together
 */
public class SocialBridge {
    private final static SocialBridge INSTANCE = new SocialBridge();
    public static SocialBridge getInstance() {
        return INSTANCE;
    }

    private final McInterface mcInterface = new McInterfaceImpl();
    private final SocialInterface socialInterface = new DiscordInterface();

    public void start() {
        LOGGER.info("Starting bridge...");

        // Load Config
        LOGGER.info("Loading Config...");
        ConfigManager.init();

        // Bot Token check
        if (ConfigManager.mainConfig.general.botToken.getDefault().equals(ConfigManager.mainConfig.general.botToken.get())) {
            LOGGER.error("A BOT TOKEN has not been provided! Please edit the config to include the bot token, then restart the server!");
            LOGGER.error("Consult the wiki for more information! The mod will disable itself!");
            return;
        }

        // Register event listeners
        // TODO

        // Start interfaces
        mcInterface.start();
        socialInterface.start();

    }
}
