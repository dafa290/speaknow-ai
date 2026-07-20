package com.speaknow.model;

public class ChatMessage {

    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String recipientName;
    private String content;
    private String timestamp;
    private Boolean isRead;

    public ChatMessage() {
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "senderId=" + senderId +
                ", senderName=" + senderName +
                ", recipientId=" + recipientId +
                ", recipientName=" + recipientName +
                ", content=" + content +
                ", timestamp=" + timestamp +
                ", isRead=" + isRead +
                '}';
    }
}
