package com.annpocoyo.apdiscordintegration.social;

import com.annpocoyo.apdiscordintegration.social.event.SocialEvent;
import com.annpocoyo.apdiscordintegration.util.ChatFormat;
import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * Abstraction layer for anything communicating with social platforms
 * Allows potential support of platforms other than Discord.
 * Mostly just for easier testing.
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
     * @param type Type of message, affects formatting
     */
    default void sendServerMessage(String message, ChatFormat type) {
        sendServerMessage(message, type, false);
    }
    /**
     * Send a message as the bot
     * @param message The message to send
     * @param type Type of message, affects formatting
     * @param suppressed If true, the message will attempt to not create push notifications
     */
    void sendServerMessage(String message, ChatFormat type, boolean suppressed);

    /**
     * Send a message as a player
     * @param message Message to send
     * @param name Name to display
     * @param type Type of message, affects formatting
     * @param pfp URL to Profile Picture to use, if applicable
     */
    default void sendPlayerMessage(String message, String name, ChatFormat type, @Nullable URI pfp) {
        sendPlayerMessage(message, name, type, pfp, false);
    }
    /**
     * Send a message as a player
     * @param message Message to send
     * @param name Name to display
     * @param type Type of message, affects formatting
     * @param pfp URL to Profile Picture to use, if available
     * @param suppressed If true, the message will attempt to not create push notifications
     */
    void sendPlayerMessage(String message, String name, ChatFormat type, @Nullable URI pfp, boolean suppressed);

    /**
     * Set the bot's status to provided and custom text to nothing
     * @param type Type of status to switch to
     */
    default void setStatus(StatusType type) {
        setStatus(type, null);
    }
    /**
     * Set the bot's status and custom text
     * @param type Type of status to switch to
     * @param message Extra text to add to the status, if applicable
     */
    void setStatus(StatusType type, @Nullable String message);

    enum StatusType {
        INVISIBLE,
        DO_NOT_DISTURB,
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
    default void stop(String lastMessage) {
        stop(lastMessage, false);
    }
    /**
     * Stop Bridge
     * @param lastMessage Message to send right before shut down.
     * @param suppressed If true, the message will attempt to not create push notifications
     */
    void stop(String lastMessage, boolean suppressed);
}
