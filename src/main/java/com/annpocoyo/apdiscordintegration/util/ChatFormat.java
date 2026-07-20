package com.annpocoyo.apdiscordintegration.util;

/**
 * Type of message, depends on the command that was (not?) used
 */
public enum ChatFormat {
    NORMAL("<%s> %s", "%s"),
    SAY("[%s] %s", "%s"),
    ME("> \\* %s %s", "\\* %s %s", true),
    GAME("> %2$s", "%s");

    private final String formatString;
    private final String formatStringFromPlayer;
    private final boolean forceNoWebhook;

    ChatFormat(String formatString, String formatStringFromPlayer) {
        this(formatString, formatStringFromPlayer, false);
    }
    ChatFormat(String formatString, String formatStringFromPlayer, boolean forceNoWebhook) {
        this.formatString = formatString;
        this.formatStringFromPlayer = formatStringFromPlayer;
        this.forceNoWebhook = forceNoWebhook;
    }

    /**
     * Get a format string to display the chat type as.
     * <p>
     * First argument should be player name, second argument should be message body.
     * @return A format string
     */
    public String getFormatString() {
        return formatString;
    }

    /**
     * Get a format string to display the chat type as, when using webhooks or other methods of making the message appear
     * to be directly from the player.
     * <p>
     * This format string is still used if {@link ChatFormat#isForceNoWebhook()} is true, but should only be used if webhooks
     * <i>would have</i> been used, and should be treated like {@link ChatFormat#getFormatString()}.
     * <p>
     * The argument should be the message body.
     * @return A format string
     */
    public String getFormatStringFromPlayer() {
        return formatStringFromPlayer;
    }

    /**
     * Should a message with this chat type always appear to be sent from the bot?
     * @return {@code true} if the message should be from the bot, {@code false} otherwise
     */
    public boolean isForceNoWebhook() {
        return forceNoWebhook;
    }
}
