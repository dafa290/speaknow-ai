package com.speaknow.controller;

import com.speaknow.model.ChatMessage;
import com.speaknow.model.Message;
import com.speaknow.model.User;
import com.speaknow.repository.MessageRepository;
import com.speaknow.repository.UserRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                   UserRepository userRepository,
                                   MessageRepository messageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        if (principal == null || principal.getName() == null || chatMessage == null) {
            return;
        }

        log.info("WS sendMessage invoked. principalName={} payloadRecipientId={}", principal.getName(), chatMessage.getRecipientId());

        Optional<User> senderOpt = userRepository.findByUsername(principal.getName());
        if (senderOpt.isEmpty() || chatMessage.getRecipientId() == null || chatMessage.getContent() == null || chatMessage.getContent().isBlank()) {
            return;
        }

        Long recipientId = java.util.Objects.requireNonNull(chatMessage.getRecipientId());
        Optional<User> recipientOpt = userRepository.findById(recipientId);
        if (recipientOpt.isEmpty()) {
            return;
        }

        User sender = senderOpt.get();
        User recipient = recipientOpt.get();

        Message saved = messageRepository.save(new Message(sender, recipient, chatMessage.getContent()));

        ChatMessage response = new ChatMessage();
        response.setSenderId(sender.getId());
        response.setSenderName(sender.getName() != null ? sender.getName() : sender.getUsername());
        response.setRecipientId(recipient.getId());
        response.setRecipientName(recipient.getName() != null ? recipient.getName() : recipient.getUsername());
        response.setContent(saved.getContent());
        response.setTimestamp(saved.getTimestamp().toString());
        response.setIsRead(saved.getIsRead());

        String recipientUsername = recipient.getUsername();
        String senderUsername = sender.getUsername();
        if (recipientUsername != null && senderUsername != null) {
            log.info("Sending WS message to recipientUsername={} and senderUsername={}", recipientUsername, senderUsername);
            messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/messages", response);
            messagingTemplate.convertAndSendToUser(senderUsername, "/queue/messages", response);
        }
    }
}
