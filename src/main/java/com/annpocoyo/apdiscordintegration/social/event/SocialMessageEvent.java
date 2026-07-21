package com.annpocoyo.apdiscordintegration.social.event;

public class SocialMessageEvent extends SocialEvent {
    private final String message;

    // User
    private final String mentionID;
    private final String username;
    private final String displayName;

    public SocialMessageEvent(String message, String mentionID, String username, String displayName) {
        this.message = message;
        this.mentionID = mentionID;
        this.username = username;
        this.displayName = displayName;
    }

    // Getters
    public String getMessage() {return message;}
    public String getMentionID() {return mentionID;}
    public String getUsername() {return username;}
    public String getDisplayName() {return displayName;}
}
