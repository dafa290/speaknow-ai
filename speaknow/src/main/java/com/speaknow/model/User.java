package com.speaknow.model;

import jakarta.persistence.*;  // ✅ PAKAI jakarta, BUKAN javax
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String name;
    private Integer totalXp;
    private Double overallScore;
    private String level;
    private Integer practiceCount;
    private Integer guidedCount;
    private Integer challengeCount;

    private Boolean isOnline = false;
    private LocalDateTime lastActive;
    private LocalDateTime createdAt;

    public User() {
        this.totalXp = 0;
        this.overallScore = 0.0;
        this.level = "Beginner";
        this.practiceCount = 0;
        this.guidedCount = 0;
        this.challengeCount = 0;
        this.name = "John Doe";
        this.isOnline = false;
        this.createdAt = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
    }

    // Getter dan Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getTotalXp() { return totalXp; }
    public void setTotalXp(Integer totalXp) { this.totalXp = totalXp; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Integer getPracticeCount() { return practiceCount; }
    public void setPracticeCount(Integer practiceCount) { this.practiceCount = practiceCount; }

    public Integer getGuidedCount() { return guidedCount; }
    public void setGuidedCount(Integer guidedCount) { this.guidedCount = guidedCount; }

    public Integer getChallengeCount() { return challengeCount; }
    public void setChallengeCount(Integer challengeCount) { this.challengeCount = challengeCount; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}