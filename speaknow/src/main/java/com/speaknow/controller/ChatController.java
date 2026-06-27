package com.speaknow.controller;

import com.speaknow.model.Message;
import com.speaknow.model.User;
import com.speaknow.repository.MessageRepository;
import com.speaknow.repository.UserRepository;
import com.speaknow.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public ChatController(MessageRepository messageRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // Kirim pesan (private endpoint, perlu login)
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
        @RequestHeader("Authorization") String bearerToken,
        @RequestBody Map<String, String> request) {

        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token tidak valid"));
        }

        String token = bearerToken.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token tidak valid atau expired"));
        }

        String senderUsername = jwtUtil.getUsernameFromToken(token);
        Long recipientId = Long.parseLong(request.getOrDefault("recipientId", "0"));
        String content = request.getOrDefault("content", "");

        if (content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Pesan tidak boleh kosong"));
        }

        // Cari sender dan recipient
        Optional<User> senderOpt = userRepository.findByUsername(senderUsername);
        Optional<User> recipientOpt = userRepository.findById(recipientId);

        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User tidak ditemukan"));
        }

        User sender = senderOpt.get();
        User recipient = recipientOpt.get();

        // Buat dan simpan pesan
        Message message = new Message(sender, recipient, content);
        messageRepository.save(message);

        return ResponseEntity.ok(Map.of(
            "message", "Pesan terkirim",
            "id", message.getId(),
            "timestamp", message.getTimestamp()
        ));
    }

    // Dapatkan percakapan antara dua user
    @GetMapping("/conversation/{userId}")
    public ResponseEntity<?> getConversation(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable Long userId) {

        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token tidak valid"));
        }

        String token = bearerToken.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token tidak valid atau expired"));
        }

        String currentUsername = jwtUtil.getUsernameFromToken(token);
        Optional<User> currentUserOpt = userRepository.findByUsername(currentUsername);
        Optional<User> otherUserOpt = userRepository.findById(userId);

        if (currentUserOpt.isEmpty() || otherUserOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User tidak ditemukan"));
        }

        User currentUser = currentUserOpt.get();
        User otherUser = otherUserOpt.get();

        // Ambil semua pesan antara kedua user
        List<Message> messages = messageRepository.findConversation(currentUser.getId(), otherUser.getId());

        // Mark pesan yang diterima sebagai sudah dibaca
        messages.stream()
            .filter(msg -> msg.getRecipient().getId().equals(currentUser.getId()))
            .forEach(msg -> {
                msg.setIsRead(true);
                messageRepository.save(msg);
            });

        // Format pesan untuk response
        List<Map<String, Object>> formattedMessages = messages.stream()
            .map(msg -> {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                msgMap.put("senderId", msg.getSender().getId());
                msgMap.put("senderName", msg.getSender().getName());
                msgMap.put("recipientId", msg.getRecipient().getId());
                msgMap.put("content", msg.getContent());
                msgMap.put("timestamp", msg.getTimestamp());
                msgMap.put("isRead", msg.getIsRead());
                return msgMap;
            })
            .collect(Collectors.toList());

        Map<String, Object> otherUserMap = new HashMap<>();
        otherUserMap.put("id", otherUser.getId());
        otherUserMap.put("username", otherUser.getUsername());
        otherUserMap.put("name", otherUser.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("otherUser", otherUserMap);
        response.put("messages", formattedMessages);
        
        return ResponseEntity.ok(response);
    }

    // Dapatkan daftar pesan yang belum dibaca
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadMessages(@RequestHeader("Authorization") String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token tidak valid"));
        }

        String token = bearerToken.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token tidak valid atau expired"));
        }

        String username = jwtUtil.getUsernameFromToken(token);
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        List<Message> unreadMessages = messageRepository.findByRecipientAndIsReadFalse(user);

        List<Map<String, Object>> formattedMessages = unreadMessages.stream()
            .map(msg -> {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                msgMap.put("senderId", msg.getSender().getId());
                msgMap.put("senderName", msg.getSender().getName());
                msgMap.put("content", msg.getContent());
                msgMap.put("timestamp", msg.getTimestamp());
                return msgMap;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("count", unreadMessages.size());
        response.put("messages", formattedMessages);
        
        return ResponseEntity.ok(response);
    }
}
