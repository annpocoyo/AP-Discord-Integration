package com.annpocoyo.apdiscordintegration.minecraft;

import com.annpocoyo.apdiscordintegration.minecraft.event.McGameMessageEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.McMessageEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartingEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppingEvent;
import com.annpocoyo.apdiscordintegration.util.ChatFormat;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

public class McInterfaceImpl implements McInterface {
    private volatile McEventHandler handler;

    // Minecraft
    private volatile MinecraftServer server;

    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<Component> sentMessages = ConcurrentHashMap.newKeySet();

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
    }

    @Override
    public void stop() {
    }

    // Operations
    @Override
    public void sendMessage(String message) {
        throwIfNotRunning();

        Component component = Component.literal(message);
        sentMessages.add(component);
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(component, false));
    }

    // Listeners
    // Server Lifecycle Events
    private void serverStarting(MinecraftServer serverInstance) {
        handler.invoke(new ServerStartingEvent());

        // Store Minecraft Server for future access
        server = serverInstance;
    }
    private void serverStarted(MinecraftServer serverInstance) {
        handler.invoke(new ServerStartedEvent());

        // Now we are running!
        running.set(true);
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
        if (sentMessages.remove(component))
            return; // Don't infinitely relay bridged messages

        handler.invoke(new McGameMessageEvent(component.getString()));
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

    private void throwIfNotRunning() throws IllegalStateException {
        if (!isRunning())
            throw new IllegalStateException("Interface not running!");
    }
}
