package com.speaknow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String mode;
    private Double finalScore;
    private Integer xpEarned;
    private LocalDateTime createdAt;

    public Session() {
        this.createdAt = LocalDateTime.now();
    }

    // Getter Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public Integer getXpEarned() { return xpEarned; }
    public void setXpEarned(Integer xpEarned) { this.xpEarned = xpEarned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userId=" + userId +
                ", mode=" + mode +
                ", finalScore=" + finalScore +
                ", xpEarned=" + xpEarned +
                ", createdAt=" + createdAt +
                '}';
    }
}