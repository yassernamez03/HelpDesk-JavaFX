package com.helpdesk.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String content;
    private boolean isUser;
    private LocalDateTime timestamp;

    public ChatMessage(String content, boolean isUser, LocalDateTime timestamp) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getContent() {
        return content;
    }

    public boolean isUser() {
        return isUser;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}