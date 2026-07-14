package com.annpocoyo.apdiscordintegration.social;

import com.annpocoyo.apdiscordintegration.social.event.SocialEvent;

/**
 * Abstraction layer for anything communicating with social platforms
 * Allows potential support of platforms other than Discord.
 */
public interface SocialInterface {
    /**
     * Begin to prepare to receive and send events.
     * @param handler Callback to handle events.
     */
    void start(SocialEventHandler handler);

    @FunctionalInterface
    interface SocialEventHandler {
        /**
         * Called on a {@code SocialEvent}
         *
         * @param event Event that occurred.
         * @return Was item added to queue successfully.
         */
        boolean invoke(SocialEvent event);
    }

    /**
     * Send a message as the bot
     * @param message The message to send
     */
    void sendMessage(String message);

    /** Whether the interface is ready to receive events */
    boolean isRunning();

    /** Stop Bridge */
    void stop();
}
