package com.annpocoyo.apdiscordintegration.social.discord;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import com.annpocoyo.apdiscordintegration.social.SocialInterface;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

public class DiscordInterface extends ListenerAdapter implements SocialInterface {
    private volatile SocialEventHandler handler;

    // Discord
    private volatile JDA jda;
    private volatile TextChannel channel;

    // State
    private final AtomicBoolean jdaObtained = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start(SocialEventHandler handler) {
        this.handler = handler;

        // Begin to connect to Discord
        JDABuilder.createLight(ConfigManager.mainConfig.general.botToken.get(), GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.customStatus("Loading..."))
                .build();
    }
    @Override
    public void stop() {
        running.set(false);
        jda.shutdown();
    }

    // Interface Implementations
    @Override
    public void sendMessage(String message) {
        if (!running.get())
            throw new IllegalStateException("Bridge is not running!");

        channel.sendMessage(message).queue();
    }

    // Listeners
    @Override
    public void onReady(ReadyEvent event) {
        jda = event.getJDA();
        jdaObtained.set(true);

        // Made contact with Discord
        // Get Discord Channel.
        if (ConfigManager.mainConfig.general.channelId == 0) {
            LOGGER.error("You have not provided a Channel ID to bind to! The bridge will not start!");
            return;
        }

        channel = jda.getTextChannelById(ConfigManager.mainConfig.general.channelId);
        if (channel == null) {
            LOGGER.error("The Channel ID is invalid! The bridge will not start!");
            return;
        }

        running.set(true);
    }

    // State Getters
    @Override
    public boolean isRunning() {
        return running.get();
    }
}
