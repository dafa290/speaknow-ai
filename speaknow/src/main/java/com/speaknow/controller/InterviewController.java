package com.speaknow.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.speaknow.model.InterviewQuestion;
import com.speaknow.model.ScoreResult;
import com.speaknow.service.InterviewService;
import com.speaknow.service.DictionaryService;

@RestController
@RequestMapping("/api/interview")
@CrossOrigin(origins = "*")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;
    
    @Autowired
    private DictionaryService dictionaryService;

    @GetMapping("/question")
    public Map<String, Object> getQuestion(@RequestParam(defaultValue = "random") String difficulty) {
        InterviewQuestion question;
        if (difficulty.equals("random")) {
            question = interviewService.getRandomQuestion();
        } else {
            question = interviewService.getQuestionByDifficulty(difficulty);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("question", question.getQuestion());
        response.put("difficulty", question.getDifficulty());
        response.put("timeLimit", question.getBaseTime());
        response.put("followUp", question.getFollowUp());
        return response;
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluate(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        String answer = (String) request.get("answer");
        String difficulty = (String) request.get("difficulty");
        
        Long userId = 1L;
        if (request.containsKey("userId")) {
            Object idObj = request.get("userId");
            if (idObj instanceof Integer) {
                userId = ((Integer) idObj).longValue();
            } else if (idObj instanceof Long) {
                userId = (Long) idObj;
            } else if (idObj instanceof String) {
                try {
                    userId = Long.parseLong((String) idObj);
                } catch (Exception e) {}
            }
        }
        
        final Long finalUserId = userId;
        if (dictionaryService != null && answer != null) {
            new Thread(() -> {
                try {
                    dictionaryService.trackUsedWords(finalUserId, answer);
                } catch (Exception e) {
                    System.out.println("⚠️ Dictionary tracking error: " + e.getMessage());
                }
            }).start();
        }
        
        ScoreResult score = interviewService.evaluateAnswer(question, answer, difficulty);
        String followUp = interviewService.getFollowUpAttack(question, answer);
        
        Map<String, Object> response = new HashMap<>();
        response.put("score", score);
        response.put("followUp", followUp);
        return response;
    }
}