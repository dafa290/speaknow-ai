package com.speaknow.controller;

import com.speaknow.model.Message;
import com.speaknow.model.User;
import com.speaknow.repository.MessageRepository;
import com.speaknow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private com.speaknow.repository.WordRepository wordRepository;

    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanup() {
        Set<String> keepUsernames = Set.of("dafa", "dafffa");

        // Ensure kept users exist
        for (String u : keepUsernames) {
            if (userRepository.findByUsername(u).isEmpty()) {
                User user = new User();
                user.setUsername(u);
                user.setEmail(u + "@example.com");
                user.setPasswordHash("" );
                user.setName(u);
                user.setIsOnline(false);
                userRepository.save(user);
            }
        }

        // Delete messages referencing non-kept users
        List<Message> messagesToDelete = messageRepository.findAll().stream()
                .filter(m -> m.getSender() == null || m.getRecipient() == null
                        || !keepUsernames.contains(m.getSender().getUsername())
                        || !keepUsernames.contains(m.getRecipient().getUsername()))
                .toList();
        int deletedMessages = messagesToDelete.size();
        if (deletedMessages > 0) messageRepository.deleteAll(messagesToDelete);

        // Delete users not in keep set (remove related word entries first)
        List<User> usersToDelete = userRepository.findAll().stream()
            .filter(u -> u.getUsername() == null || !keepUsernames.contains(u.getUsername()))
            .toList();
        int deletedUsers = 0;
        if (!usersToDelete.isEmpty()) {
            List<Long> idsToDelete = usersToDelete.stream().map(User::getId).toList();
            List<com.speaknow.model.WordEntry> wordsToDelete = wordRepository.findByUserIdIn(idsToDelete);
            if (!wordsToDelete.isEmpty()) wordRepository.deleteAll(wordsToDelete);
            deletedUsers = usersToDelete.size();
            userRepository.deleteAll(usersToDelete);
        }

        return ResponseEntity.ok(Map.of(
                "kept", keepUsernames,
                "deletedUsers", deletedUsers,
                "deletedMessages", deletedMessages
        ));
    }
}
