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

    // Operations
    /**
     * Send a message as the bot
     * @param message The message to send
     */
    void sendServerMessage(String message);
    /**
     * Send a message as the bot
     * @param message The message to send
     * @param suppressed If true, the message will attempt to not create push notifications
     */
    void sendServerMessage(String message, boolean suppressed);

    /**
     * Set the bot's status to provided and custom text to nothing
     * @param type Type of status to switch to
     */
    void setStatus(StatusType type);
    /**
     * Set the bot's status and custom text
     * @param type Type of status to switch to
     * @param message Extra text to add to the status, if applicable
     */
    void setStatus(StatusType type, String message);

    enum StatusType {
        INVISIBLE,
        DND,
        IDLE,
        ONLINE
    }

    /** Whether the interface is ready to receive events */
    boolean isRunning();

    /** Stop Bridge */
    void stop();
    /**
     * Stop Bridge
     * @param lastMessage Message to send right before shut down.
     */
    void stop(String lastMessage);
    /**
     * Stop Bridge
     * @param lastMessage Message to send right before shut down.
     * @param suppressed If true, the message will attempt to not create push notifications
     */
    void stop(String lastMessage, boolean suppressed);
}
