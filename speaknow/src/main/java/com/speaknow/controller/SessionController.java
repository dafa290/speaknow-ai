package com.speaknow.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.speaknow.model.ChatRequest;
import com.speaknow.model.ChatResponse;
import com.speaknow.model.Session;
import com.speaknow.model.SessionRequest;
import com.speaknow.model.SessionResponse;
import com.speaknow.model.User;
import com.speaknow.repository.SessionRepository;
import com.speaknow.repository.UserRepository;
import com.speaknow.service.AIService;

/**
 * REST Controller for handling user learning sessions.
 * Manages Free Chat, Guided Mode, and Challenge Mode sessions.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SessionController {

    @Autowired
    private AIService aiService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @PostMapping("/session/save")
    public Map<String, Object> saveSession(@RequestBody Map<String, Object> request) {
        String mode = (String) request.getOrDefault("mode", "practice");
        Long userId = 1L;
        if (request.containsKey("userId")) {
            userId = Long.valueOf(request.get("userId").toString());
        }
        double finalScore = 0.0;
        if (request.containsKey("finalScore")) {
            finalScore = Double.valueOf(request.get("finalScore").toString());
        }

        int xp = switch (mode) {
            case "practice" -> 5;
            case "guided" -> 10;
            case "challenge" -> 20;
            default -> 0;
        };
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setTotalXp(user.getTotalXp() + xp);
            
            if (mode.equals("practice")) {
                user.setPracticeCount(user.getPracticeCount() + 1);
            } else if (mode.equals("guided")) {
                user.setGuidedCount(user.getGuidedCount() + 1);
            } else if (mode.equals("challenge")) {
                user.setChallengeCount(user.getChallengeCount() + 1);
            }
            
            double newOverall = (user.getOverallScore() + finalScore) / 2;
            user.setOverallScore(Math.min(newOverall, 100));
            
            if (user.getOverallScore() >= 71) {
                user.setLevel("Advanced");
            } else if (user.getOverallScore() >= 41) {
                user.setLevel("Intermediate");
            } else {
                user.setLevel("Beginner");
            }
            
            userRepository.save(user);
            
            Session session = new Session();
            session.setUserId(user.getId());
            session.setMode(mode);
            session.setFinalScore(finalScore);
            session.setXpEarned(xp);
            sessionRepository.save(session);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("xpEarned", xp);
        return response;
    }

    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElse(null);
    }
    
    @GetMapping("/user/{id}/sessions")
    public List<Session> getUserSessions(@PathVariable Long id) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(id);
    }
    
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return aiService.chat(request);
    }

    @PostMapping("/hint")
    public Map<String, String> getHint(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        String userMessage = request.get("message");
        String lastAIMessage = request.get("lastAIMessage");
        String mode = request.get("mode");
        response.put("hint", aiService.generateHintOnly(userMessage, lastAIMessage, mode));
        return response;
    }

    @PostMapping("/alternative")
    public ChatResponse getAlternative(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        ChatResponse response = new ChatResponse();
        Map<String, String> styles = aiService.generateStyleAnswers(message);
        response.setNaturalAnswer(styles.get("natural"));
        response.setAcademicAnswer(styles.get("ielts"));
        response.setCasualAnswer(styles.get("slang"));
        return response;
    }

    @PostMapping("/grammar-check")
    public Map<String, Object> checkGrammar(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        return aiService.checkGrammarWithScore(text);
    }

    @PostMapping("/translate")
    public Map<String, String> translateMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String type = request.get("type"); // "smart" or "full"
        Long userId = Long.valueOf(request.getOrDefault("userId", "1"));
        
        String translation = aiService.translateMessage(message, type, userId);
        Map<String, String> response = new HashMap<>();
        response.put("translation", translation);
        return response;
    }
}