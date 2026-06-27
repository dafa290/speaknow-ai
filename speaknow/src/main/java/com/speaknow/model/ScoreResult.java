package com.speaknow.model;

public class ScoreResult {
    private int grammar;
    private int clarity;
    private int confidence;
    private String comment;
    private String feedback;

    public ScoreResult() {}

    public ScoreResult(int grammar, int clarity, int confidence, String comment, String feedback) {
        this.grammar = grammar;
        this.clarity = clarity;
        this.confidence = confidence;
        this.comment = comment;
        this.feedback = feedback;
    }

    // Getters & Setters
    public int getGrammar() { return grammar; }
    public void setGrammar(int grammar) { this.grammar = grammar; }

    public int getClarity() { return clarity; }
    public void setClarity(int clarity) { this.clarity = clarity; }

    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}