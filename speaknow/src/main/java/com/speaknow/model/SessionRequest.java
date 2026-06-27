package com.speaknow.model;

public class SessionRequest {
    private String question;
    private String userAnswer;
    private String mode; // practice, guided, challenge
    private Long userId;

    public SessionRequest() {}

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}