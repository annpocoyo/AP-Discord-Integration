package com.annpocoyo.apdiscordintegration.social.discord;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import com.annpocoyo.apdiscordintegration.social.SocialInterface;
import com.annpocoyo.apdiscordintegration.social.event.SocialMessageEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.SocialInvalidTokenEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.SocialNoChannelEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.SocialReadyEvent;
import com.annpocoyo.apdiscordintegration.util.ChatFormat;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

public class DiscordInterface extends ListenerAdapter implements SocialInterface {
    private volatile SocialEventHandler handler;

    // Discord
    private volatile JDA jda;
    private volatile TextChannel channel;
    private volatile Webhook webhook = null;
    private volatile SelfUser self;
    private volatile Presence presence;

    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean jdaObtained = new AtomicBoolean(false);

    // Multipurpose executor
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start(SocialEventHandler handler) {
        this.handler = handler;

        // Begin to connect to Discord
        LOGGER.info("Starting JDA...");
        try {
            jda = JDABuilder.createLight(ConfigManager.mainConfig.general.botToken.get(),
                            GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(this)
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setActivity(Activity.customStatus("Loading..."))
                    .build();
            jdaObtained.set(true);
        } catch (InvalidTokenException _) {
            LOGGER.error("The Discord Token provided is invalid, the bridge will not start!");
            handler.invoke(new SocialInvalidTokenEvent());
        }
    }
    @Override
    public void stop() {
        LOGGER.info("Shutting down JDA...");
        running.set(false);
        executor.shutdown();
        if (jdaObtained.get()) {
            jda.shutdown();
        }
    }
    @Override
    public void stop(String lastMessage, boolean suppressed) {
        if (running.get()) {
            String message = ConfigManager.mainConfig.features.useWebhooks ?
                    ChatFormat.GAME.getFormatStringFromPlayer().formatted("Server Stopped") :
                    ChatFormat.GAME.getFormatString().formatted("Server", "Server Stopped");
            channel.sendMessage(message).setSuppressedNotifications(suppressed).complete();
        }
        stop();
    }

    // Interface Implementations
    @Override
    public void setStatus(StatusType type, String message) {
        throwIfNotRunning();

        executor.execute(() -> presence.setPresence(OnlineStatus.valueOf(type.name()), message != null ? Activity.customStatus(message) : null));
    }

    @Override
    public void sendServerMessage(String message, ChatFormat type, boolean suppressed) {
        throwIfNotRunning();

        // TODO: definitely take this into its own method (maybe inside of ChatFormat?).
        String formattedMessage = ConfigManager.mainConfig.features.useWebhooks ?
                (type.isForceNoWebhook() ? type.getFormatStringFromPlayer().formatted("Server", message) :
                        type.getFormatStringFromPlayer().formatted(message)) :
                type.getFormatString().formatted("Server", message);

        channel.sendMessage(formattedMessage).setSuppressedNotifications(suppressed).queue();
    }

    @Override
    public void sendPlayerMessage(String message, String name, ChatFormat type, @Nullable URI pfp, boolean suppressed) {
        if (!ConfigManager.mainConfig.features.useWebhooks || type.isForceNoWebhook()) {
            String formatString = ConfigManager.mainConfig.features.useWebhooks ?
                    type.getFormatStringFromPlayer() :
                    type.getFormatString();
            channel.sendMessage(formatString.formatted(name, message)).setSuppressedNotifications(suppressed).queue();
            return;
        }

        // 'DISCORD' and 'CLYDE' are disallowed to be contained within a webhook display name.
        String parsedName = name.replaceAll("/Discord|Clyde/gi", "######");
        if (!parsedName.equals(name))
            LOGGER.warn("Username was censored due to containing a disallowed keyword (Original: {}, Censored: {})",
                    message, parsedName);

        webhook.sendMessage(type.getFormatStringFromPlayer().formatted(message)).setUsername(name)
                .setSuppressedNotifications(suppressed).queue();
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

        if (ConfigManager.mainConfig.features.useWebhooks) {
            webhook = channel.retrieveWebhooks().complete().stream().filter(currentWebhook -> { // Find existing webhook
                var owner = currentWebhook.getOwnerAsUser();
                if (owner == null)
                    return false;
                return (owner.getIdLong() == self.getIdLong());
            }).findFirst().orElseGet(() -> channel.createWebhook("AP Minecraft Bridge").complete()); // Create a new webhook if needed
        }

        running.set(true);
        handler.invoke(new SocialReadyEvent());
    }

    // Listeners
    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = event.getAuthor();

        if (event.getChannel().getIdLong() != channel.getIdLong())
            return; // Different Channel
        if (author.getIdLong() == self.getIdLong() ||
                (ConfigManager.mainConfig.features.useWebhooks && webhook.getDefaultUser().getIdLong() == author.getIdLong()))
            return; // Self sent message, don't want to relay forever.

        // We know that we can probably get a Member now
        Member member = Objects.requireNonNull(event.getMember());

        handler.invoke(new SocialMessageEvent(message.getContentStripped(),
                author.getAsMention(), author.getName(), member.getEffectiveName()));
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
