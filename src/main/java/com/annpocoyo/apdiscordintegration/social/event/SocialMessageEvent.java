package com.annpocoyo.apdiscordintegration.social.event;

public class SocialMessageEvent extends SocialEvent {
    private final String message;

    // User
    private final long userID;
    private final String username;
    private final String displayName;

    public SocialMessageEvent(String message, long userID, String username, String displayName) {
        this.message = message;
        this.userID = userID;
        this.username = username;
        this.displayName = displayName;
    }

    // Getters
    public String getMessage() {return message;}
    public long getUserID() {return userID;}
    public String getUsername() {return username;}
    public String getDisplayName() {return displayName;}
}
