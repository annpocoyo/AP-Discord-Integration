package com.annpocoyo.apdiscordintegration.minecraft.event.players;

public class McPlayerJoined extends McPlayerCountChanged {
    public McPlayerJoined(int newPlayerCount) {
        super(newPlayerCount);
    }
}
