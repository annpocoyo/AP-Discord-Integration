package com.annpocoyo.apdiscordintegration;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APDiscordIntegration implements ModInitializer {
	public static final String MOD_ID = "ap-discord-integration";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			LOGGER.error("This mod only works on dedicated servers. Singleplayer Worlds are not supported! The mod will disable itself!");
			return;
		}

		// Load Config
		LOGGER.info("Loading Config...");
		ConfigManager.init();

		if (ConfigManager.mainConfig.general.botToken.getDefault().equals(ConfigManager.mainConfig.general.botToken.get())) {
			LOGGER.error("A BOT TOKEN has not been provided! Please edit the config to include the bot token, then restart the server!");
			LOGGER.error("Consult the wiki for more information! The mod will disable itself!");
		}

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
