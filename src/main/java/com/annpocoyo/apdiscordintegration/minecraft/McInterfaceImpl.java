package com.annpocoyo.apdiscordintegration.minecraft;

import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStartingEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppedEvent;
import com.annpocoyo.apdiscordintegration.minecraft.event.lifecycle.ServerStoppingEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicBoolean;

public class McInterfaceImpl implements McInterface {
    private volatile McEventHandler handler;

    // Minecraft
    private volatile MinecraftServer server;

    // State
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start(McEventHandler handler) {
        this.handler = handler;

        // Register events
        ServerLifecycleEvents.SERVER_STARTING.register(this::serverStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(this::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::serverStopped);
    }
    @Override
    public void stop() {
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
    private void serverStopping(MinecraftServer serverInstance) {handler.invoke(new ServerStoppingEvent());}
    private void serverStopped(MinecraftServer serverInstance) {
        // Now we are not running!
        running.set(false);

        handler.invoke(new ServerStoppedEvent());
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
