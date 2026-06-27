package com.speaknow.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speaknow.model.*;

@Service
public class InterviewService {

    @Value("${groq.api.key}")
    private String GROQ_API_KEY;
    private final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    // Database pertanyaan interview (bisa ditambah)
    private List<InterviewQuestion> questionBank = Arrays.asList(
        new InterviewQuestion("Tell me about yourself.", "easy", 45, "That's too generic. What specific skills make you unique?"),
        new InterviewQuestion("What is your biggest weakness?", "medium", 60, "That's a common answer. Give me a real example."),
        new InterviewQuestion("Why should we hire you?", "medium", 60, "Be more specific. What value do you bring?"),
        new InterviewQuestion("Where do you see yourself in 5 years?", "medium", 50, "That's vague. What role or position?"),
        new InterviewQuestion("Describe a time you failed.", "hard", 70, "What did you learn from that failure?"),
        new InterviewQuestion("How do you handle pressure?", "medium", 55, "Give me a specific situation."),
        new InterviewQuestion("Why did you leave your last job?", "hard", 65, "Be honest but professional."),
        new InterviewQuestion("What are your salary expectations?", "easy", 40, "Give me a range, not just a number.")
    );

    private Random random = new Random();

    public InterviewQuestion getRandomQuestion() {
        return questionBank.get(random.nextInt(questionBank.size()));
    }

    public InterviewQuestion getQuestionByDifficulty(String difficulty) {
        List<InterviewQuestion> filtered = questionBank.stream()
            .filter(q -> q.getDifficulty().equals(difficulty))
            .toList();
        return filtered.get(random.nextInt(filtered.size()));
    }

    public ScoreResult evaluateAnswer(String question, String answer, String difficulty) {
        try {
            String prompt = String.format(
                "You are a strict HR interviewer. Evaluate this answer strictly and honestly.\n" +
                "Question: '%s'\n" +
                "Answer: '%s'\n" +
                "Rate from 0-10 for:\n" +
                "1. Grammar (sentence structure, tenses, word choice)\n" +
                "2. Clarity (is the answer clear and easy to understand?)\n" +
                "3. Confidence (does it sound confident or hesitant?)\n\n" +
                "Also give:\n" +
                "- A short honest comment (1 sentence, critical if bad)\n" +
                "- Feedback for improvement (1 sentence)\n\n" +
                "Return ONLY in format: GRAMMAR:X|CLARITY:X|CONFIDENCE:X|COMMENT:...|FEEDBACK:...",
                question, answer
            );

            String aiResult = callGroq(prompt);
            
            // Parse response
            int grammar = extractValue(aiResult, "GRAMMAR:");
            int clarity = extractValue(aiResult, "CLARITY:");
            int confidence = extractValue(aiResult, "CONFIDENCE:");
            String comment = extractText(aiResult, "COMMENT:", "FEEDBACK:");
            String feedback = extractAfter(aiResult, "FEEDBACK:");

            return new ScoreResult(grammar, clarity, confidence, comment, feedback);

        } catch (Exception e) {
            // Fallback jika API error
            return new ScoreResult(5, 5, 4, 
                "Your answer needs improvement.", 
                "Be more specific and use stronger vocabulary.");
        }
    }

    public String getFollowUpAttack(String originalQuestion, String userAnswer) {
        try {
            String prompt = String.format(
                "The user gave this answer: '%s'\n" +
                "For interview question: '%s'\n" +
                "Generate a challenging follow-up question that pushes the user to be more specific.\n" +
                "Be strict but fair. Return only the follow-up question.",
                userAnswer, originalQuestion
            );
            return callGroq(prompt);
        } catch (Exception e) {
            return "Can you give me a more specific example?";
        }
    }

    private String callGroq(String prompt) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 200);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(GROQ_API_KEY);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            return "Your answer could be improved. Try to be more specific and confident.";
        }
    }

    private int extractValue(String text, String key) {
        try {
            int start = text.indexOf(key) + key.length();
            int end = text.indexOf("|", start);
            if (end == -1) end = text.length();
            String value = text.substring(start, end).trim();
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 5;
        }
    }

    private String extractText(String text, String startKey, String endKey) {
        try {
            int start = text.indexOf(startKey) + startKey.length();
            int end = text.indexOf(endKey, start);
            if (end == -1) end = text.length();
            return text.substring(start, end).trim();
        } catch (Exception e) {
            return "Your answer needs improvement.";
        }
    }

    private String extractAfter(String text, String key) {
        try {
            int start = text.indexOf(key) + key.length();
            return text.substring(start).trim();
        } catch (Exception e) {
            return "Keep practicing!";
        }
    }
}