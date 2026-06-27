package com.speaknow.repository;

import com.speaknow.model.Message;
import com.speaknow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // Cari semua pesan antara dua user (diurutkan dari yang terbaru)
    @Query(value = "SELECT * FROM messages WHERE " +
           "(sender_id = :userId1 AND recipient_id = :userId2) OR " +
           "(sender_id = :userId2 AND recipient_id = :userId1) " +
           "ORDER BY timestamp DESC", nativeQuery = true)
    List<Message> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // Cari pesan yang diterima user tertentu dan belum dibaca
    List<Message> findByRecipientAndIsReadFalse(User recipient);

    // Cari semua pesan yang diterima user tertentu
    List<Message> findByRecipientOrderByTimestampDesc(User recipient);

    // Cari pesan dari user tertentu
    List<Message> findBySenderOrderByTimestampDesc(User sender);
}
