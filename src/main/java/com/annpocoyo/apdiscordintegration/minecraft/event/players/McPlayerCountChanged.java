package com.annpocoyo.apdiscordintegration.minecraft.event.players;

import com.annpocoyo.apdiscordintegration.minecraft.event.McEvent;

public class McPlayerCountChanged extends McEvent {
    private final int newPlayerCount;

    public McPlayerCountChanged(int newPlayerCount) {
        this.newPlayerCount = newPlayerCount;
    }

    /**
     * @return Player Count now that the player has left or joined.
     */
    public int getNewPlayerCount() {
        return newPlayerCount;
    }
}
