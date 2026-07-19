package com.annpocoyo.apdiscordintegration.minecraft;

import com.annpocoyo.apdiscordintegration.minecraft.event.McEvent;

/**
 * Abstraction layer for anything contacting Minecraft
 * Intended to be used to facilitate future multi version support!
 */
public interface McInterface {
    /**
     * Begin to prepare to receive and send events.
     * @param handler Callback to handle events.
     */
    void start(McEventHandler handler);

    @FunctionalInterface
    interface McEventHandler {
        /**
         * Called on an 'McEvent'
         * @param event Event that occurred.
         * @return Was item added to queue successfully.
         */
        boolean invoke(McEvent event);
    }

    // Operations
    /**
     * Send a message directly to the MC chat
     * @param message The message to send
     */
    void sendMessage(String message);

    /**
     * Get the number of players online
     * @return Number of players online
     */
    int getPlayerCount();

    /** Whether the interface is ready to receive events */
    boolean isRunning();

    /** Stop Bridge */
    void stop();
}
