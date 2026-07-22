package com.annpocoyo.apdiscordintegration.social.entities;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class SocialMessage {
    private final String message;
    private final SocialUser author;
    @Nullable private final SocialMessage replyMessage;

    public SocialMessage(String message, SocialUser author, @Nullable SocialMessage replyMessage) {
        this.message = message;
        this.author = author;
        this.replyMessage = replyMessage;
    }

    // Getters
    public String getMessage() {return message;}
    public SocialUser getAuthor() {return author;}
    public Optional<SocialMessage> getReplyMessage() {return Optional.ofNullable(replyMessage);}
}
