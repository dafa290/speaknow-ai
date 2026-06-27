package com.speaknow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.speaknow.model.ConversationHistory;

public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {
    List<ConversationHistory> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);
}