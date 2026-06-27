package com.speaknow.model;

public class InterviewQuestion {
    private String question;
    private String difficulty; // easy, medium, hard
    private int baseTime; // waktu dasar dalam detik
    private String followUp; // follow-up question jika jawaban terlalu generic

    public InterviewQuestion() {}

    public InterviewQuestion(String question, String difficulty, int baseTime, String followUp) {
        this.question = question;
        this.difficulty = difficulty;
        this.baseTime = baseTime;
        this.followUp = followUp;
    }

    // Getters & Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getBaseTime() { return baseTime; }
    public void setBaseTime(int baseTime) { this.baseTime = baseTime; }

    public String getFollowUp() { return followUp; }
    public void setFollowUp(String followUp) { this.followUp = followUp; }
}