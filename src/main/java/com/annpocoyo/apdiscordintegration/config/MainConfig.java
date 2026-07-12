package com.annpocoyo.apdiscordintegration.config;

import com.annpocoyo.apdiscordintegration.APDiscordIntegration;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.annotations.LiteralString;
import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import org.jspecify.annotations.NonNull;

@Version(version = 0)
public class MainConfig extends Config {
    public MainConfig() {
        super(APDiscordIntegration.id("main"));
    }

    public General general = new General();
    public static class General extends ConfigSection {
        @LiteralString
        @Comment("Provide the bot token for the bot account that will be controlled by this mod! YOU NEED TO CHANGE THIS FOR THE BOT TO WORK!")
        public ValidatedString botToken = new ValidatedString("INSERT BOT TOKEN HERE");

        @Comment("ID of channel to be bridged to Minecraft. Can be found by right-clicking a channel while in Developer Mode.")
        public long channelId = 0;
    }

    public Advanced advanced = new Advanced();
    public static class Advanced extends ConfigSection {
        @Comment("Disable auto bot configuration? Do not change unless you know what you're doing!")
        public boolean disableAutoBotConfig = false;
    }

    @Override
    public @NonNull SaveType saveType() {
        return SaveType.SEPARATE;
    }
}
