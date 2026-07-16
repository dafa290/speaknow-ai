package com.speaknow.service;

import com.speaknow.model.User;
import com.speaknow.model.Message;
import com.speaknow.repository.UserRepository;
import com.speaknow.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Component responsible for seeding initial database data.
 * Cleans up unused test accounts to maintain database integrity.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private com.speaknow.repository.WordRepository wordRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Keep only these two test accounts; remove other test accounts to keep DB clean
        java.util.Set<String> keepUsernames = java.util.Set.of("dafa", "dafffa");

        // Ensure kept users exist
        seedUserIfMissing("dafa", "dafa@example.com", "Password123!", "Dafa");
        seedUserIfMissing("dafffa", "dafffa@example.com", "Password123!", "Dafffa");

        // Delete messages that reference users not in keep set
        java.util.List<Message> messagesToDelete = messageRepository.findAll().stream()
                .filter(m -> m.getSender() == null || m.getRecipient() == null
                        || !keepUsernames.contains(m.getSender().getUsername())
                        || !keepUsernames.contains(m.getRecipient().getUsername()))
                .toList();
        if (!messagesToDelete.isEmpty()) {
            messageRepository.deleteAll(messagesToDelete);
            System.out.println("🗑️ Deleted " + messagesToDelete.size() + " messages referencing removed users.");
        }

        // Delete users not in keep set (remove dependent user_words first)
        java.util.List<User> usersToDelete = userRepository.findAll().stream()
                .filter(u -> u.getUsername() == null || !keepUsernames.contains(u.getUsername()))
                .toList();
        if (!usersToDelete.isEmpty()) {
            java.util.List<Long> idsToDelete = usersToDelete.stream().map(User::getId).toList();
            java.util.List<com.speaknow.model.WordEntry> wordsToDelete = wordRepository.findByUserIdIn(idsToDelete);
            if (!wordsToDelete.isEmpty()) {
                wordRepository.deleteAll(wordsToDelete);
                System.out.println("🗑️ Deleted " + wordsToDelete.size() + " word entries for removed users.");
            }
            userRepository.deleteAll(usersToDelete);
            System.out.println("🗑️ Deleted " + usersToDelete.size() + " non-essential users.");
        }

        System.out.println("✅ Database cleaned. Kept users: " + keepUsernames);
    }

    private void seedUserIfMissing(String username, String email, String rawPassword, String name) {
        if (userRepository.findByUsername(username).isEmpty()) {
            System.out.println("🌱 Seeding user: " + username);
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setName(name);
            user.setIsOnline(false);
            userRepository.save(user);
            System.out.println("✅ Created user " + username + " with password " + rawPassword);
        }
    }
}
