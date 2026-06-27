package com.speaknow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.speaknow.model.WordEntry;

@Repository
public interface WordRepository extends JpaRepository<WordEntry, Long> {
    
    Optional<WordEntry> findByUserIdAndWord(Long userId, String word);
    
    List<WordEntry> findAllByUserIdAndWord(Long userId, String word);
    
    List<WordEntry> findByUserIdOrderByLastUsedDesc(Long userId);
    
    List<WordEntry> findByUserIdAndMasteredFalseOrderByUsageCountDesc(Long userId);
    
    @Query("SELECT COUNT(w) FROM WordEntry w WHERE w.userId = :userId AND w.mastered = true")
    int countMasteredByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(w) FROM WordEntry w WHERE w.userId = :userId")
    int countTotalByUser(@Param("userId") Long userId);

    // Find entries for multiple users
    java.util.List<WordEntry> findByUserIdIn(java.util.List<Long> userIds);
}