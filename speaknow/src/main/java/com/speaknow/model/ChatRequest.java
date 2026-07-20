package com.speaknow.model;

public class ChatRequest {
    private Long userId;
    private String sessionId;
    private String message;
    private String mode;

    public ChatRequest() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "userId=" + userId +
                ", sessionId=" + sessionId +
                ", message=" + message +
                ", mode=" + mode +
                '}';
    }
}