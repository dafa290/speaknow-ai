package com.speaknow.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_words")
public class WordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String word;
    private String meaning;
    private String category;
    private String example;
    private int usageCount;
    private boolean mastered;
    private LocalDateTime firstUsed;
    private LocalDateTime lastUsed;

    public WordEntry() {
        this.usageCount = 0;
        this.mastered = false;
        this.firstUsed = LocalDateTime.now();
        this.lastUsed = LocalDateTime.now();
    }

    // ========== GETTERS & SETTERS ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public boolean isMastered() { return mastered; }
    public void setMastered(boolean mastered) { this.mastered = mastered; }

    public LocalDateTime getFirstUsed() { return firstUsed; }
    public void setFirstUsed(LocalDateTime firstUsed) { this.firstUsed = firstUsed; }

    public LocalDateTime getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
}