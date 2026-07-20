package com.speaknow.model;

public class ChatResponse {
    private String message;
    private String grammarFeedback;
    private String hint;
    private String naturalAnswer;
    private String academicAnswer;
    private String casualAnswer;
    private String[] topicSuggestions;

    public ChatResponse() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getGrammarFeedback() { return grammarFeedback; }
    public void setGrammarFeedback(String grammarFeedback) { this.grammarFeedback = grammarFeedback; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public String getNaturalAnswer() { return naturalAnswer; }
    public void setNaturalAnswer(String naturalAnswer) { this.naturalAnswer = naturalAnswer; }

    public String getAcademicAnswer() { return academicAnswer; }
    public void setAcademicAnswer(String academicAnswer) { this.academicAnswer = academicAnswer; }

    public String getCasualAnswer() { return casualAnswer; }
    public void setCasualAnswer(String casualAnswer) { this.casualAnswer = casualAnswer; }

    public String[] getTopicSuggestions() { return topicSuggestions; }
    public void setTopicSuggestions(String[] topicSuggestions) { this.topicSuggestions = topicSuggestions; }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "message=" + message +
                ", grammarFeedback=" + grammarFeedback +
                ", hint=" + hint +
                ", naturalAnswer=" + naturalAnswer +
                ", academicAnswer=" + academicAnswer +
                ", casualAnswer=" + casualAnswer +
                '}';
    }
}