package com.speaknow.controller;

import com.speaknow.model.User;
import com.speaknow.repository.UserRepository;
import com.speaknow.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for managing user profiles and online status.
 * Provides public and private endpoints for user data retrieval.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public UserController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // Public endpoint: dapatkan daftar semua user
    @GetMapping("/discover")
    public ResponseEntity<?> discoverUsers() {
        List<User> allUsers = userRepository.findAllByOrderByCreatedAtDesc();
        
        List<Map<String, Object>> users = allUsers.stream()
            .map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("name", user.getName());
                userMap.put("level", user.getLevel());
                userMap.put("totalXp", user.getTotalXp());
                userMap.put("isOnline", user.getIsOnline());
                return userMap;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("count", users.size());
        response.put("users", users);
        return ResponseEntity.ok(response);
    }

    // Public endpoint: dapatkan user yang sedang online
    @GetMapping("/online")
    public ResponseEntity<?> getOnlineUsers() {
        List<User> onlineUsers = userRepository.findAllByIsOnlineTrueOrderByLastActiveDesc();
        
        List<Map<String, Object>> users = onlineUsers.stream()
            .map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("name", user.getName());
                userMap.put("level", user.getLevel());
                userMap.put("totalXp", user.getTotalXp());
                return userMap;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("count", users.size());
        response.put("users", users);
        return ResponseEntity.ok(response);
    }

    // Public endpoint: dapatkan profile user berdasarkan ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("level", user.getLevel());
        response.put("totalXp", user.getTotalXp());
        response.put("overallScore", user.getOverallScore());
        response.put("practiceCount", user.getPracticeCount());
        response.put("guidedCount", user.getGuidedCount());
        response.put("challengeCount", user.getChallengeCount());
        response.put("isOnline", user.getIsOnline());
        response.put("createdAt", user.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }

    // Private endpoint: update profile user (hanya bisa dilakukan user sendiri)
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
        @RequestHeader("Authorization") String bearerToken,
        @RequestBody Map<String, String> request) {

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

        // Update field yang diperbolehkan
        if (request.containsKey("name") && !request.get("name").isBlank()) {
            user.setName(request.get("name"));
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile berhasil diperbarui"));
    }
}
