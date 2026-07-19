package com.annpocoyo.apdiscordintegration.util;

/**
 * Type of message, depends on the command that was (not?) used
 */
public enum ChatFormat {
    NORMAL("<%s> %s", "%s"),
    SAY("[%s] %s", "%s"),
    ME("\\* %s %s", "*%s*"),
    GAME("> %s", "> %s");

    private final String formatString;
    private final String formatStringFromPlayer;

    ChatFormat(String formatString, String formatStringFromPlayer) {
        this.formatString = formatString;
        this.formatStringFromPlayer = formatStringFromPlayer;
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
     * The argument should be the message body.
     * @return A format string
     */
    public String getFormatStringFromPlayer() {
        return formatStringFromPlayer;
    }
}
