package com.helpdesk.model;

import java.time.LocalDateTime;

public class ConversationEntry {
    private String userMessage;
    private String botResponse;
    private LocalDateTime timestamp;
    private boolean helpful;

    public ConversationEntry(String userMessage, String botResponse, LocalDateTime timestamp, boolean helpful) {
        this.userMessage = userMessage;
        this.botResponse = botResponse;
        this.timestamp = timestamp;
        this.helpful = helpful;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getBotResponse() {
        return botResponse;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isHelpful() {
        return helpful;
    }
}