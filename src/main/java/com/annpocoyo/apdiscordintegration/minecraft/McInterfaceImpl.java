package com.annpocoyo.apdiscordintegration.minecraft;

import com.annpocoyo.apdiscordintegration.minecraft.event.McGameMessageEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.McMessageEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartingEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppingEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.players.McPlayerJoined;
import com.annpocoyo.apdiscordintegration.minecraft.event.players.McPlayerLeaved;
import com.annpocoyo.apdiscordintegration.social.entities.SocialMessage;
import com.annpocoyo.apdiscordintegration.social.entities.SocialUser;
import com.annpocoyo.apdiscordintegration.util.ChatFormat;
import com.annpocoyo.apdiscordintegration.util.GlobalTellRawEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

public class McInterfaceImpl implements McInterface {
    private volatile McEventHandler handler;

    // Minecraft
    private volatile MinecraftServer server;

    // State
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Message Components
    private final static Component discordTag = Component.literal("[Discord]")
            .withStyle(ChatFormatting.BOLD)
            .withColor(0x5865F2)
            .withStyle(Style.EMPTY.withHoverEvent(
                    new HoverEvent.ShowText(
                            Component.literal("This message is from the ")
                                    .append(Component.literal("Discord Bridge").withStyle(ChatFormatting.BOLD))
                                    .append("!")
                    )
            ));
    private final static Component botTag = Component.literal("{BOT}")
            .withColor(TextColor.AQUA)
            .withStyle(Style.EMPTY.withHoverEvent(
                    new HoverEvent.ShowText(
                            Component.literal("This message was sent by a ")
                                    .append(Component.literal("BOT").withStyle(ChatFormatting.BOLD))
                                    .append("!")
                    )
            ));

    @Override
    public void start(McEventHandler handler) {
        this.handler = handler;

        // Register events
        // Lifecycle
        ServerLifecycleEvents.SERVER_STARTING.register(this::serverStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(this::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::serverStopped);
        // Messages
        ServerMessageEvents.CHAT_MESSAGE.register(this::chatMessage);
        ServerMessageEvents.COMMAND_MESSAGE.register(this::commandMessage);
        ServerMessageEvents.GAME_MESSAGE.register(this::gameMessage);
        GlobalTellRawEvent.register(this::tellRawMessage);
        // Players
        ServerPlayerEvents.JOIN.register(this::playerJoin);
        ServerPlayerEvents.LEAVE.register(this::playerLeave);
    }

    @Override
    public void stop() {
        this.handler = _ -> true;
    }

    // Operations
    @Override
    public void sendMessage(String message) {
        throwIfNotRunning();

        Component component = Component.empty()
                .append(discordTag)
                .append(" ")
                .append(message);
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(component, false));
    }

    @Override
    public void sendSpecialMessage(SpecialMessageType type) {
        throwIfNotRunning();
        Component message;
        switch (type) {
            case BRIDGE_RECONNECT ->
                    message = Component.literal("Bridge ")
                            .append(Component.literal("Reconnected")
                                    .withStyle(ChatFormatting.GREEN))
                            .append("!")
                            .withStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent.ShowText(
                                            Component.literal("Messages are ")
                                                    .append(Component.literal("now")
                                                            .withStyle(ChatFormatting.BOLD))
                                                    .append(" being bridged to the other side again!")
                                    ))
                            );
            case BRIDGE_DISCONNECT ->
                    message = Component.literal("Bridge ")
                            .append(Component.literal("Disconnected")
                                    .withStyle(ChatFormatting.RED))
                            .append("!")
                            .withStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent.ShowText(
                                            Component.literal("Messages are ")
                                                    .append(Component.literal("no longer")
                                                            .withStyle(ChatFormatting.BOLD))
                                                    .append(" being bridged to the other side!")
                                    ))
                            );
            default -> {
                LOGGER.warn("Unknown Special Message Type: {}", type);
                message = Component.literal("Unknown Message Type")
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
            }
        }

        Component component = Component.empty()
                .append(discordTag)
                .append(" ")
                .append(message);
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(component, false));
    }

    @Override
    public void sendSocialMessage(SocialMessage message) {
        throwIfNotRunning();

        // Create components
        Component socialName = convertSocialUserComponent(message.getAuthor());
        Component messageContents = Component.literal(message.getMessage());

        // Replies
        Component replyMessageComponent = Component.empty();
        if (message.getReplyMessage().isPresent()) {
            SocialMessage replyMessage = message.getReplyMessage().orElseThrow();
            Component replySocialName = convertSocialUserComponent(replyMessage.getAuthor());

            replyMessageComponent = Component.empty()
                    .append(Component.literal("(In reply to <")
                            .withColor(TextColor.GRAY))
                    .append(replySocialName)
                    .append(Component.literal(">) ")
                            .withColor(TextColor.GRAY));
        }

        // Put them together and send
        Component finalMessage = Component.empty()
                .append(discordTag)
                .append(" <").append(socialName).append("> ")
                .append(replyMessageComponent)
                .append(messageContents);
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(finalMessage, false));
    }

    @Override
    public CompletableFuture<Integer> getPlayerCount() {
        throwIfNotRunning();
        return server.submit(() -> server.getPlayerCount());
    }

    // Listeners
    // Server Lifecycle Events
    private void serverStarting(MinecraftServer serverInstance) {
        // Store Minecraft Server for future access
        server = serverInstance;

        handler.invoke(new ServerStartingEvent());

    }
    private void serverStarted(MinecraftServer serverInstance) {
        // Now we are running!
        running.set(true);

        handler.invoke(new ServerStartedEvent());
    }
    private void serverStopping(MinecraftServer serverInstance) {
        // Now we are not running!
        running.set(false);

        handler.invoke(new ServerStoppingEvent());
    }
    private void serverStopped(MinecraftServer serverInstance) {handler.invoke(new ServerStoppedEvent());}
    // Chat Messages
    private void chatMessage(PlayerChatMessage playerChatMessage, ServerPlayer serverPlayer, ChatType.Bound bound) {
        handler.invoke(new McMessageEvent(playerChatMessage.signedContent(), convertBoundToType(bound), serverPlayer.getPlainTextName()));
    }
    private void commandMessage(PlayerChatMessage playerChatMessage, CommandSourceStack commandSourceStack, ChatType.Bound bound) {
        String playerName = commandSourceStack.isPlayer() ?
                Objects.requireNonNull(commandSourceStack.getPlayer()).getPlainTextName() : null;
        handler.invoke(new McMessageEvent(playerChatMessage.signedContent(), convertBoundToType(bound), playerName));
    }
    private void gameMessage(MinecraftServer minecraftServer, Component component, boolean overlay) {
        if (component.contains(discordTag))
            return; // Don't infinitely relay bridged messages

        handler.invoke(new McGameMessageEvent(component.getString()));
    }
    private void tellRawMessage(Component component, CommandSourceStack commandSourceStack) {
        handler.invoke(new McGameMessageEvent(component.getString()));
    }
    // Players
    private void playerJoin(ServerPlayer serverPlayer) {
        if (!running.get()) return;
        handler.invoke(new McPlayerJoined(
                server.getPlayerCount()
        ));
    }
    private void playerLeave(ServerPlayer serverPlayer) {
        if (!running.get()) return;
        handler.invoke(new McPlayerLeaved(
                server.getPlayerCount() - 1 // Event is triggered before player count is updated, so account for that
        ));
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    // Util
    private static ChatFormat convertBoundToType(ChatType.Bound bound) {
        if (bound.chatType().is(ChatType.CHAT))
            return ChatFormat.NORMAL;
        else if (bound.chatType().is(ChatType.SAY_COMMAND))
            return ChatFormat.SAY;
        else if (bound.chatType().is(ChatType.EMOTE_COMMAND))
            return ChatFormat.ME;
        else {
            LOGGER.warn("Unknown ChatType: {}, assuming that it is NORMAL", bound.chatType().getRegisteredName());
            return ChatFormat.NORMAL;
        }
    }

    private static @NonNull MutableComponent convertSocialUserComponent(SocialUser author) {
        return Component.literal(author.displayName())
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("@" + author.username())
                                        .append("\n")
                                        .append(Component.literal("Click")
                                                .withStyle(ChatFormatting.BOLD))
                                        .append(" to ")
                                        .append(Component.literal("mention")
                                                .withStyle(ChatFormatting.ITALIC))
                                        .append("!")
                        ))
                        .withClickEvent(new ClickEvent.SuggestCommand(author.mentionID() + " "))
                        .withInsertion(author.mentionID())
                )
                .append(author.isBot() ? Component.literal(" ").append(botTag) : Component.empty());
    }

    private void throwIfNotRunning() throws IllegalStateException {
        if (!isRunning())
            throw new IllegalStateException("Interface not running!");
    }
}
