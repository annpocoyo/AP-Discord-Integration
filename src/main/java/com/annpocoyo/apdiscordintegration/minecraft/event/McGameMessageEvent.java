package com.annpocoyo.apdiscordintegration.minecraft.event;

public class McGameMessageEvent extends McEvent {
    private final String message;

    public McGameMessageEvent(String message) {
        this.message = message;
    }

    // Getters
    public String getMessage() {return message;}
}
