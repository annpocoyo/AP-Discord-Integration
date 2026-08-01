package com.annpocoyo.apdiscordintegration.social.discord;

import com.annpocoyo.apdiscordintegration.config.ConfigManager;
import com.annpocoyo.apdiscordintegration.social.SocialInterface;
import com.annpocoyo.apdiscordintegration.social.entities.SocialMessage;
import com.annpocoyo.apdiscordintegration.social.entities.SocialUser;
import com.annpocoyo.apdiscordintegration.social.event.SocialMessageEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.BridgeDisconnectEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.BridgeReconnectEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.startup.SocialInvalidTokenEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.startup.SocialNoChannelEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.startup.SocialReadyEvent;
import com.annpocoyo.apdiscordintegration.social.event.lifecycle.startup.SocialUnknownErrorEvent;
import com.annpocoyo.apdiscordintegration.util.ChatFormat;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

public class DiscordInterface extends ListenerAdapter implements SocialInterface {
    private volatile SocialEventHandler handler;

    // Discord
    private volatile JDA jda;
    private volatile TextChannel channel;
    private volatile long webhookId; // In case of cache invalidation
    private volatile Webhook webhook = null;
    private volatile SelfUser self;
    private volatile Presence presence;

    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean jdaObtained = new AtomicBoolean(false);
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

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
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error occurred while connecting to Discord: ",e);
            handler.invoke(new SocialUnknownErrorEvent());
        }
    }
    @Override
    public void stop() {
        LOGGER.info("Shutting down JDA...");
        running.set(false);
        executor.shutdownNow();
        if (jdaObtained.get()) {
            jda.shutdownNow();
        }
        this.handler = _ -> true;
    }
    @Override
    public void stop(String lastMessage, boolean suppressed) {
        try {
            if (running.get()) {
                channel.sendMessage(ChatFormat.GAME.format(
                        "Server", "Server Stopped", ConfigManager.mainConfig.features.useWebhooks
                )).setSuppressedNotifications(suppressed).complete(false); // Send Immediately
            }
        } catch (RateLimitedException _) {
            LOGGER.warn("Server Stopped Message was rate limited");
        } catch (RuntimeException e) {
            LOGGER.warn("Unknown exception occurred while sending last message: ", e);
        } finally {
            stop();
        }
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
        channel.sendMessage(type.format(
                "Server", message, ConfigManager.mainConfig.features.useWebhooks
        )).setSuppressedNotifications(suppressed).queue();
    }

    @Override
    public void sendPlayerMessage(String message, String name, ChatFormat type, @Nullable URI pfp, boolean suppressed) {
        String formattedMessage = type.format(
                name, message, ConfigManager.mainConfig.features.useWebhooks
        );
        if (!ConfigManager.mainConfig.features.useWebhooks || type.forceNoWebhook()) {
            channel.sendMessage(formattedMessage).setSuppressedNotifications(suppressed).queue();
            return;
        }

        // 'DISCORD' and 'CLYDE' are disallowed to be contained within a webhook display name.
        String parsedName = name.replaceAll("/Discord|Clyde/gi", "######");
        if (!parsedName.equals(name))
            LOGGER.warn("Username was censored due to containing a disallowed keyword (Original: {}, Censored: {})",
                    message, parsedName);

        webhook.sendMessage(formattedMessage).setUsername(parsedName)
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
            webhookId = webhook.getIdLong();
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

        // Get Display Name
        Member member = event.getMember();
        String effectiveName = member != null ? member.getEffectiveName() : author.getEffectiveName();

        // Handle Replies
        SocialMessage replyMessage = null;
        MessageReference messageReference = message.getMessageReference();
        if (messageReference != null) {
            try {
                Message resolvedMessage = messageReference.resolve().complete();
                User replyAuthor = resolvedMessage.getAuthor();

                Member replyMember = resolvedMessage.getMember();
                String replyEffectiveName = replyMember != null ? replyMember.getEffectiveName() : replyAuthor.getEffectiveName();

                replyMessage = new SocialMessage(resolvedMessage.getContentStripped(),
                        new SocialUser(replyAuthor.getAsMention(), replyAuthor.getName(), replyEffectiveName, replyAuthor.isBot()),
                        null);
            } catch (InsufficientPermissionException _) {
                LOGGER.warn("Missing permission to access MESSAGE_HISTORY, cannot resolve reply, falling back to assuming message isn't a reply");
            } catch (ErrorResponseException e) {
                LOGGER.warn("Failed to get message replied to (is it deleted now?), falling back to assuming message isn't a reply", e);
            }
        }

        SocialMessage socialMessage = new SocialMessage(message.getContentStripped(),
                new SocialUser(author.getAsMention(), author.getName(), effectiveName, author.isBot()),
                replyMessage);
        handler.invoke(new SocialMessageEvent(socialMessage));
    }

    @Override
    public void onSessionDisconnect(@NonNull SessionDisconnectEvent event) {
        if (disconnected.compareAndSet(false, true)) {
            handler.invoke(new BridgeDisconnectEvent());
        }
    }

    @Override
    public void onSessionInvalidate(@NonNull SessionInvalidateEvent event) {
        if (disconnected.get()) {
            LOGGER.warn("Session Invalidated whilest disconnected!");
            return;
        }

        LOGGER.info("Session invalidated, recreating objects...");
        recreateObjects();
    }

    @Override
    public void onSessionRecreate(@NonNull SessionRecreateEvent event) {
        if (!disconnected.compareAndSet(true, false))
            return;

        LOGGER.info("Session recreated, recreating objects...");
        recreateObjects();
        handler.invoke(new BridgeReconnectEvent());
    }

    @Override
    public void onSessionResume(@NonNull SessionResumeEvent event) {
        if (!disconnected.compareAndSet(true, false))
            return;

        handler.invoke(new BridgeReconnectEvent());
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

    private void recreateObjects() {
        channel = jda.getTextChannelById(ConfigManager.mainConfig.general.channelId);
        if (channel == null) {
            LOGGER.error("The Channel ID is now invalid!? The bridge will shut down!");
            handler.invoke(new SocialNoChannelEvent());
            return;
        }
        webhook = jda.retrieveWebhookById(webhookId).complete();
        self = jda.getSelfUser();
        presence = jda.getPresence();
    }
}
