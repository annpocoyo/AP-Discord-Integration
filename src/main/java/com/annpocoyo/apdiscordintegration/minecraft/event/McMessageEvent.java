package com.annpocoyo.apdiscordintegration.minecraft.event;

import com.annpocoyo.apdiscordintegration.util.ChatFormat;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class McMessageEvent extends McEvent {
    private final String message;
    private final ChatFormat type;

    // Player
    private final @Nullable String playerName;

    public McMessageEvent(String message, ChatFormat type) {this(message, type, null);}
    public McMessageEvent(String message, ChatFormat type, @Nullable String playerName) {
        this.message = message;
        this.type = type;
        this.playerName = playerName;
    }

    // Getters
    public String getMessage() {return message;}
    public ChatFormat getType() {return type;}
    /** If empty, is from console */
    public Optional<String> getPlayerName() {return Optional.ofNullable(playerName);}
}
