package com.annpocoyo.apdiscordintegration.util;

import static com.annpocoyo.apdiscordintegration.APDiscordIntegration.LOGGER;

/**
 * Type of message, depends on the command that was (not?) used
 */
public enum ChatFormat {
    NORMAL,
    SAY,
    ME,
    GAME;

    /**
     * Use the provided name and message contents to create a formatted message
     * <p>
     * Also does proper handling for 'self sent' messages
     * @param author Name of author
     * @param message Contents of message
     * @param webhooksEnabled Whether 'self sent' messages like webhook based messages are enabled.
     */
    public String format(String author, String message, boolean webhooksEnabled) {
        if (webhooksEnabled)
            switch (this) {
                case NORMAL, SAY, GAME -> {
                    return message;
                }
                case ME -> {
                    return "\\* %s %s".formatted(author, message);
                }
            }
        else
            switch (this) {
                case NORMAL -> {
                    return "<%s> %s".formatted(author, message);
                }
                case SAY -> {
                    return "[%s] %s".formatted(author, message);
                }
                case ME -> {
                    return "> \\* %s %s".formatted(author, message);
                }
                case GAME -> {
                    return "> %s".formatted(message);
                }
            }
        LOGGER.warn("Message (Type: {}, Author: {}, Contents: {}) somehow wasn't formatted? Falling back to '<%s> %s'...",
                this, author, message);
        return "<%s> %s".formatted(author, message);
    }

    /**
     * Should a non webhook message be forced?
     * @return {@code true} if yes, {@code false} if no
     */
    public boolean forceNoWebhook() {
        return (this == ME); // Don't ruin the /me illusion
    }
}
