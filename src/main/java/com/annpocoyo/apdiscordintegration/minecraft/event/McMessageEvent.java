package com.annpocoyo.apdiscordintegration.minecraft.event;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class McMessageEvent extends McEvent {
    private final String message;
    private final ChatType type;

    // Player
    private final @Nullable String playerName;

    public McMessageEvent(String message, ChatType type) {this(message, type, null);}
    public McMessageEvent(String message, ChatType type, @Nullable String playerName) {
        this.message = message;
        this.type = type;
        this.playerName = playerName;
    }

    // Getters
    public String getMessage() {return message;}
    public ChatType getType() {return type;}
    /** If empty, is from console */
    public Optional<String> getPlayerName() {return Optional.ofNullable(playerName);}

    /**
     * Type of message, depends on the command that was (not?) used
     */
    public enum ChatType {
        NORMAL,
        SAY,
        ME
    }
}
