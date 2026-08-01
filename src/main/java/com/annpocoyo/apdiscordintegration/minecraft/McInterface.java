package com.annpocoyo.apdiscordintegration.minecraft;

import com.annpocoyo.apdiscordintegration.minecraft.event.McEvent;
import com.annpocoyo.apdiscordintegration.social.entities.SocialMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction layer for anything contacting Minecraft
 * Intended to be used to facilitate future multi version support!
 * Mostly just for easier testing.
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
     * Sends a special message to the MC chat
     * @param type Type of special message to send
     */
    void sendSpecialMessage(SpecialMessageType type);

    enum SpecialMessageType {
        BRIDGE_DISCONNECT,
        BRIDGE_RECONNECT
    }

    /**
     * Send a message as a Social User to the MC chat
     * @param message Social Message object to send
     */
    void sendSocialMessage(SocialMessage message);

    /**
     * Get the number of players online
     * @return Number of players online
     */
    CompletableFuture<Integer> getPlayerCount();

    /** Whether the interface is ready to receive events */
    boolean isRunning();

    /** Stop Bridge */
    void stop();
}
