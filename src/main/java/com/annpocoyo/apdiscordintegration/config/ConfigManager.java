package com.annpocoyo.apdiscordintegration.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;

public class ConfigManager {
    public static MainConfig mainConfig;

    public static void init() {
        mainConfig = ConfigApiJava.registerAndLoadConfig(MainConfig::new);
    }
}
