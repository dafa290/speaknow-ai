package com.speaknow.model;

public class SessionResponse {
    private Integer grammarScore;
    private Integer coherenceScore;
    private Integer speedScore;
    private Double finalScore;
    private String correction;
    private String naturalAnswer;
    private String academicAnswer;
    private String casualAnswer;
    private String hint;
    private Integer xpEarned;

    public SessionResponse() {}

    // Getter dan Setter
    public Integer getGrammarScore() { return grammarScore; }
    public void setGrammarScore(Integer grammarScore) { this.grammarScore = grammarScore; }

    public Integer getCoherenceScore() { return coherenceScore; }
    public void setCoherenceScore(Integer coherenceScore) { this.coherenceScore = coherenceScore; }

    public Integer getSpeedScore() { return speedScore; }
    public void setSpeedScore(Integer speedScore) { this.speedScore = speedScore; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getCorrection() { return correction; }
    public void setCorrection(String correction) { this.correction = correction; }

    public String getNaturalAnswer() { return naturalAnswer; }
    public void setNaturalAnswer(String naturalAnswer) { this.naturalAnswer = naturalAnswer; }

    public String getAcademicAnswer() { return academicAnswer; }
    public void setAcademicAnswer(String academicAnswer) { this.academicAnswer = academicAnswer; }

    public String getCasualAnswer() { return casualAnswer; }
    public void setCasualAnswer(String casualAnswer) { this.casualAnswer = casualAnswer; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public Integer getXpEarned() { return xpEarned; }
    public void setXpEarned(Integer xpEarned) { this.xpEarned = xpEarned; }

    @Override
    public String toString() {
        return "SessionResponse{" +
                "grammarScore=" + grammarScore +
                ", coherenceScore=" + coherenceScore +
                ", speedScore=" + speedScore +
                ", finalScore=" + finalScore +
                ", correction=" + correction +
                ", naturalAnswer=" + naturalAnswer +
                ", academicAnswer=" + academicAnswer +
                ", casualAnswer=" + casualAnswer +
                ", hint=" + hint +
                ", xpEarned=" + xpEarned +
                '}';
    }
}