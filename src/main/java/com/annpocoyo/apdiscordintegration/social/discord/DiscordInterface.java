package com.annpocoyo.apdiscordintegration.social.discord;

import com.annpocoyo.apdiscordintegration.social.SocialInterface;

public class DiscordInterface implements SocialInterface {
    @Override
    public void start() {
        //
    }

    // Events
    // Ready Event
    ReadyEvent readyEvent = new ReadyEvent();
    @Override
    public ReadyEvent getReadyEvent() {
        return readyEvent;
    }
}
