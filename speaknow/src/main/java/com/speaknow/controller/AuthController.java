package com.speaknow.controller;

import com.speaknow.model.User;
import com.speaknow.repository.UserRepository;
import com.speaknow.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        String name = request.getOrDefault("name", username);

        // Validasi input
        if (username == null || email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username, email, dan password wajib diisi"));
        }

        // Cek apakah username atau email sudah terdaftar
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username sudah terdaftar"));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email sudah terdaftar"));
        }

        // Buat user baru
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setName(name);
        newUser.setIsOnline(false);

        userRepository.save(newUser);

        // Generate JWT token
        String token = jwtUtil.generateToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registrasi berhasil");
        response.put("token", token);
        response.put("user", Map.of(
            "id", newUser.getId(),
            "username", newUser.getUsername(),
            "email", newUser.getEmail(),
            "name", newUser.getName()
        ));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // Validasi input
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username dan password wajib diisi"));
        }

        // Cari user berdasarkan username
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username atau password salah"));
        }

        User user = userOpt.get();

        // Validasi password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username atau password salah"));
        }

        // Update status online
        user.setIsOnline(true);
        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login berhasil");
        response.put("token", token);
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "name", user.getName(),
            "level", user.getLevel(),
            "totalXp", user.getTotalXp()
        ));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setIsOnline(false);
                userRepository.save(user);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Logout berhasil"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String bearerToken) {
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
            return ResponseEntity.badRequest().body(Map.of("error", "User tidak ditemukan"));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "name", user.getName(),
            "level", user.getLevel(),
            "totalXp", user.getTotalXp(),
            "isOnline", user.getIsOnline()
        ));
    }
}
