package com.annpocoyo.apdiscordintegration.social.event;

import com.annpocoyo.apdiscordintegration.social.entities.SocialMessage;

public class SocialMessageEvent extends SocialEvent {
    private final SocialMessage message;

    public SocialMessageEvent(SocialMessage message) {
        this.message = message;
    }

    // Getters
    public SocialMessage getMessage() {return message;}
}
