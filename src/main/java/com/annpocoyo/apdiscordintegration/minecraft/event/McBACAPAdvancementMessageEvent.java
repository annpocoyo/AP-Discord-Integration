package com.annpocoyo.apdiscordintegration.minecraft.event;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class McBACAPAdvancementMessageEvent extends McEvent {
    private final String message;
    private final String description;
    private final @Nullable String tab;

    public McBACAPAdvancementMessageEvent(String message, String description, @Nullable String tab) {
        this.message = message;
        this.description = description;
        this.tab = tab;
    }

    public String getMessage() {return message;}
    public String getDescription() {return description;}
    public Optional<String> getTab() {return Optional.ofNullable(tab);}
}
