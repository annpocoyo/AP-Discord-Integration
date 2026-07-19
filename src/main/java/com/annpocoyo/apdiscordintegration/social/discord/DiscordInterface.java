package com.annpocoyo.apdiscordintegration.social.discord;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import com.annpocoyo.apdiscordintegration.social.SocialInterface;
import com.annpocoyo.apdiscordintegration.social.event.SocialMessageEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.SocialNoChannelEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.SocialReadyEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

public class DiscordInterface extends ListenerAdapter implements SocialInterface {
    private volatile SocialEventHandler handler;

    // Discord
    private volatile JDA jda;
    private volatile TextChannel channel;
    private volatile SelfUser self;
    private volatile Presence presence;

    // State
    private final AtomicBoolean jdaObtained = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Multipurpose executor
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start(SocialEventHandler handler) {
        this.handler = handler;

        // Begin to connect to Discord
        LOGGER.info("Starting JDA...");
        jda = JDABuilder.createLight(ConfigManager.mainConfig.general.botToken.get(),
                        GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(this)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.customStatus("Loading..."))
                .build();
        jdaObtained.set(true);
    }
    @Override
    public void stop() {
        LOGGER.info("Shutting down JDA...");
        running.set(false);
        executor.shutdown();
        jda.shutdown();
    }
    @Override
    public void stop(String lastMessage) {
        stop(lastMessage, false);
    }
    @Override
    public void stop(String lastMessage, boolean suppressed) {
        if (running.get()) {
            channel.sendMessage(lastMessage).setSuppressedNotifications(suppressed).complete();
        }
        stop();
    }

    // Interface Implementations
    @Override
    public void setStatus(StatusType type) {
        setStatus(type, null);
    }
    @Override
    public void setStatus(StatusType type, String message) {
        throwIfNotRunning();

        var status = switch (type) {
            case INVISIBLE -> OnlineStatus.INVISIBLE;
            case DND -> OnlineStatus.DO_NOT_DISTURB;
            case IDLE -> OnlineStatus.IDLE;
            case ONLINE -> OnlineStatus.ONLINE;
        };

        executor.execute(() -> presence.setPresence(status, message != null ? Activity.customStatus(message) : null));
    }

    @Override
    public void sendServerMessage(String message) {
        sendServerMessage(message, false);}
    @Override
    public void sendServerMessage(String message, boolean suppressed) {
        throwIfNotRunning();

        channel.sendMessage(message).setSuppressedNotifications(suppressed).queue();
    }

    // Listeners
    @Override
    public void onReady(@NonNull ReadyEvent event) {
        // Made contact with Discord
        // Get Discord Channel.
        if (ConfigManager.mainConfig.general.channelId == 0) {
            LOGGER.error("You have not provided a Channel ID to bind to! The bridge will not start!");
            handler.invoke(new SocialNoChannelEvent());
            return;
        }

        channel = jda.getTextChannelById(ConfigManager.mainConfig.general.channelId);
        if (channel == null) {
            LOGGER.error("The Channel ID is invalid! The bridge will not start!");
            handler.invoke(new SocialNoChannelEvent());
            return;
        }

        self = jda.getSelfUser();
        presence = jda.getPresence();

        running.set(true);
        handler.invoke(new SocialReadyEvent());
    }

    // Listeners
    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != channel.getIdLong())
            return; // Different Channel
        if (event.getAuthor().getIdLong() == self.getIdLong())
            return; // Self sent message, don't want to relay forever.

        Message message = event.getMessage();
        User author = event.getAuthor();

        handler.invoke(new SocialMessageEvent(message.getContentStripped(),
                author.getIdLong(), author.getName(), author.getEffectiveName()));
    }

    // State Getters
    @Override
    public boolean isRunning() {
        return running.get();
    }

    // Util
    private void throwIfNotRunning() throws IllegalStateException {
        if (!isRunning())
            throw new IllegalStateException("Interface not running!");
    }
}
