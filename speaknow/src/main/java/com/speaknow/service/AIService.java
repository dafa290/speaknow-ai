package com.speaknow.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speaknow.model.ChatRequest;
import com.speaknow.model.ChatResponse;
import com.speaknow.model.ConversationHistory;
import com.speaknow.model.SessionRequest;
import com.speaknow.model.SessionResponse;
import com.speaknow.repository.ConversationHistoryRepository;

@Service
public class AIService {

    @Autowired
    private DictionaryService dictionaryService;
    
    @Autowired
    private ConversationHistoryRepository historyRepo;

    @Value("${groq.api.key}")
    private String GROQ_API_KEY;
    private final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    // ========== CHAT MODE ==========
public ChatResponse chat(ChatRequest request) {
    Long userId = request.getUserId();
    String sessionId = request.getSessionId();
    String userMessage = request.getMessage();
    String mode = request.getMode();

    saveMessage(userId, sessionId, "user", userMessage);

    List<ConversationHistory> history = historyRepo.findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);
    String userLevel = getUserLevel(userId);
    
    String aiResponse;
    if ("voice".equals(mode) || "practice".equals(mode)) {
        aiResponse = callGroq(userMessage, mode, userLevel, history, "chat", userId);
        String[] words = aiResponse.split(" ");
        if (words.length > 20) {
            aiResponse = String.join(" ", java.util.Arrays.copyOf(words, 20)) + "...";
        }
    } else {
        aiResponse = callGroq(userMessage, mode, userLevel, history, "chat", userId);
    }
    
    saveMessage(userId, sessionId, "assistant", aiResponse);

    // 🔥 PINDAHKAN TRACKING KE SINI (SETELAH AI RESPON, JADI ASYNC)
    // User sudah dapet response, tracking jalan di background
    if (dictionaryService != null) {
        // HANYA track user message, BUKAN AI response
        new Thread(() -> {
            try {
                dictionaryService.trackUsedWords(userId, userMessage);
            } catch (Exception e) {
                System.out.println("⚠️ Dictionary tracking error: " + e.getMessage());
            }
        }).start();
    }

    ChatResponse response = new ChatResponse();
    response.setMessage(aiResponse);
    response.setGrammarFeedback(null);
    response.setHint(null);
    response.setNaturalAnswer(null);
    response.setAcademicAnswer(null);
    response.setCasualAnswer(null);
    
    if (!"voice".equals(mode)) {
        response.setTopicSuggestions(generateTopicSuggestions(mode, aiResponse, history));
    }

    return response;
}

    // ========== HINT ONLY ==========
    public String generateHintOnly(String userMessage, String lastAIMessage, String mode) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a helpful English tutor. Based on the conversation, give 1 short hint (1 sentence) to help the user respond to the AI's last question. Be specific and useful. Start with 💡 Hint:");
            messages.add(systemMsg);
            
            Map<String, String> contextMsg = new HashMap<>();
            contextMsg.put("role", "user");
            contextMsg.put("content", "AI's last question: \"" + lastAIMessage + "\"\nMy last message: \"" + userMessage + "\"\n\nGive me a hint on how to respond to the AI's question.");
            messages.add(contextMsg);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 100);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(GROQ_API_KEY);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            String hint = root.path("choices").get(0).path("message").path("content").asText();
            
            hint = hint.replace("💡 Hint:", "").trim();
            if (!hint.startsWith("💡")) hint = "💡 Hint: " + hint;
            return hint;
            
        } catch (Exception e) {
            return "💡 Hint: Try answering the AI's question with a complete sentence!";
        }
    }
    
    // ========== TRANSLATION ==========
    public String translateMessage(String message, String type, Long userId) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        if (isGroqKeyMissing()) {
            return translateMessageFallback(message, type, userId);
        }

        try {
            if ("smart".equals(type)) {
                List<com.speaknow.model.WordEntry> userWords = dictionaryService.getUserWords(userId);
                java.util.Set<String> knownWords = new java.util.HashSet<>();
                for (com.speaknow.model.WordEntry w : userWords) {
                    knownWords.add(w.getWord().toLowerCase());
                }
                
                String[] words = message.toLowerCase().split("\\s+");
                java.util.Set<String> unknownWords = new java.util.HashSet<>();
                for(String w : words) {
                    w = w.replaceAll("[^a-z]", "");
                    if(w.length() > 2 && !dictionaryService.isStopWord(w) && !knownWords.contains(w)) {
                        unknownWords.add(w);
                    }
                }
                
                if (unknownWords.isEmpty()) {
                    return "✅ Kosakata sudah familier bagi Anda (tidak ada kata sulit/baru).";
                }
                
                String wordsToTranslate = String.join(", ", unknownWords);
                return executeTranslationGroq("Translate ONLY these specific English words to Indonesian: " + wordsToTranslate + ". Format as a bulleted list: '- Word: Translation'");
            } else {
                return executeTranslationGroq("Translate the following English text to Indonesian accurately and naturally:\n\n" + message);
            }
        } catch (Exception e) {
            System.err.println("Translation error, calling fallback: " + e.getMessage());
            return translateMessageFallback(message, type, userId);
        }
    }
    
    private String executeTranslationGroq(String prompt) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 200);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(GROQ_API_KEY);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            System.err.println("Groq Translation API Error: " + e.getMessage());
            e.printStackTrace();
            return "Translation failed.";
        }
    }

    // ========== GRAMMAR CHECK (3 SCORES: Grammar, Clarity, Confidence) ==========
    public Map<String, Object> checkGrammarWithScore(String text) {
        if (text == null || text.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("grammarScore", 5);
            result.put("clarityScore", 5);
            result.put("confidenceScore", 5);
            result.put("feedback", "Analisis tidak tersedia");
            return result;
        }

        if (isGroqKeyMissing()) {
            return checkGrammarRuleBased(text);
        }
        
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "Anda adalah penilai Bahasa Inggris yang SANGAT KETAT, KRITIS, dan OBJEKTIF. Tugas Anda mencari kesalahan grammar (tense, subject-verb, ejaan, typo, tanda baca). JANGAN PERNAH memberi nilai 10 jika ada kesalahan sekecil apa pun.\n" +
                   "Wajib mengembalikan HANYA format JSON (tanpa markdown):\n" +
                   "{\"grammarScore\": <0-10>, \"clarityScore\": <0-10>, \"confidenceScore\": <0-10>, \"feedback\": \"<max 3 kalimat Bahasa Indonesia>\"}\n\n" +
                   "PANDUAN SKOR (WAJIB DIIKUTI):\n" +
                   "- grammarScore: Jika banyak typo atau tata bahasa berantakan/ngaco, berikan skor 1-4. Jika ada salah tense/verb/struktur, skor maksimal 6. Hanya berikan 10 jika benar-benar sempurna layaknya penutur asli tingkat lanjut.\n" +
                   "- clarityScore: Seberapa jelas maknanya. Kalimat kacau = skor rendah.\n" +
                   "- confidenceScore: Penggunaan kosakata yang tepat.\n\n" +
                   "FEEDBACK:\n" +
                   "- Jelaskan dengan tegas bagian kalimat mana yang salah (misal: 'goes' seharusnya 'went').\n" +
                   "- Berikan kalimat yang benar.");
            messages.add(systemMsg);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "Analisis: \"" + text + "\"");
            messages.add(userMsg);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 250);
            
            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(GROQ_API_KEY);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            String aiResult = root.path("choices").get(0).path("message").path("content").asText();
            
            System.out.println("Grammar Check Response: " + aiResult);
            
            // Clean response
            aiResult = aiResult.trim();
            if (aiResult.startsWith("```json")) aiResult = aiResult.substring(7);
            if (aiResult.startsWith("```")) aiResult = aiResult.substring(3);
            if (aiResult.endsWith("```")) aiResult = aiResult.substring(0, aiResult.length() - 3);
            aiResult = aiResult.trim();
            
            // Parse JSON
            Map<String, Object> result = new HashMap<>();
            try {
                JsonNode jsonNode = objectMapper.readTree(aiResult);
                
                if (jsonNode.has("grammarScore")) {
                    int score = jsonNode.get("grammarScore").asInt();
                    result.put("grammarScore", Math.max(0, Math.min(10, score)));
                } else {
                    result.put("grammarScore", 5);
                }
                if (jsonNode.has("clarityScore")) {
                    int score = jsonNode.get("clarityScore").asInt();
                    result.put("clarityScore", Math.max(0, Math.min(10, score)));
                } else {
                    result.put("clarityScore", 5);
                }
                if (jsonNode.has("confidenceScore")) {
                    int score = jsonNode.get("confidenceScore").asInt();
                    result.put("confidenceScore", Math.max(0, Math.min(10, score)));
                } else {
                    result.put("confidenceScore", 5);
                }
                if (jsonNode.has("feedback")) {
                    result.put("feedback", jsonNode.get("feedback").asText());
                } else {
                    result.put("feedback", "Analisis grammar selesai.");
                }
            } catch (Exception parseError) {
                System.out.println("JSON parse failed, extracting manually");
                result.put("grammarScore", 5);
                result.put("clarityScore", 5);
                result.put("confidenceScore", 5);
                result.put("feedback", aiResult);
            }
            return result;
            
        } catch (Exception e) {
            System.err.println("Groq grammar check failed, calling fallback: " + e.getMessage());
            return checkGrammarRuleBased(text);
        }
    }

    // ========== ALTERNATIVE STYLES ==========
    public Map<String, String> generateStyleAnswers(String text) {
        if (text == null || text.trim().isEmpty()) {
            Map<String, String> result = new HashMap<>();
            result.put("natural", "");
            result.put("ielts", "");
            result.put("slang", "");
            return result;
        }

        if (isGroqKeyMissing()) {
            return generateStyleAnswersRuleBased(text);
        }
        
        Map<String, String> result = new HashMap<>();
        result.put("natural", text);
        result.put("ielts", text);
        result.put("slang", text);
        
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a text rewriter. REWRITE the user's sentence, NOT answer it.\n" +
                   "Rewrite this exact sentence in 3 different styles:\n" +
                   "NATURAL: friendly daily conversation, fix grammar\n" +
                   "IELTS: formal, academic, sophisticated\n" +
                   "SLANG: casual, relaxed, everyday speech\n" +
                   "IMPORTANT: Do NOT answer the question. Just rewrite the sentence.\n" +
                   "Return ONLY in JSON format: {\"natural\": \"...\", \"ielts\": \"...\", \"slang\": \"...\"}");
            messages.add(systemMsg);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "Rewrite this sentence: \"" + text + "\"");
            messages.add(userMsg);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 200);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(GROQ_API_KEY);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            String aiResult = root.path("choices").get(0).path("message").path("content").asText();
            
            // Parse JSON response
            try {
                JsonNode jsonNode = objectMapper.readTree(aiResult);
                if (jsonNode.has("natural")) result.put("natural", jsonNode.get("natural").asText());
                if (jsonNode.has("ielts")) result.put("ielts", jsonNode.get("ielts").asText());
                if (jsonNode.has("slang")) result.put("slang", jsonNode.get("slang").asText());
            } catch (Exception e) {
                // Fallback ke format lama
                if (aiResult.contains("NATURAL:") && aiResult.contains("IELTS:") && aiResult.contains("SLANG:")) {
                    String natural = extractBetween(aiResult, "NATURAL:", "IELTS:");
                    String ielts = extractBetween(aiResult, "IELTS:", "SLANG:");
                    String slang = aiResult.substring(aiResult.indexOf("SLANG:") + 6).trim();
                    if (!natural.isEmpty()) result.put("natural", natural);
                    if (!ielts.isEmpty()) result.put("ielts", ielts);
                    if (!slang.isEmpty()) result.put("slang", slang);
                }
            }
        } catch (Exception e) {
            System.err.println("Groq style rewrite failed, calling fallback: " + e.getMessage());
            return generateStyleAnswersRuleBased(text);
        }
        return result;
    }

    private String getUserLevel(Long userId) {
        return "Intermediate";
    }

        private String callGroq(String userMessage, String mode, String level, List<ConversationHistory> history, String purpose, Long userId) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            
            List<String> targetWords = new ArrayList<>();
            if (dictionaryService != null && userId != null) {
                targetWords = dictionaryService.getUnusedTargetWords(userId);
            }
            
            String systemPrompt = getSystemPrompt(mode, level, purpose, targetWords);
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
            
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                ConversationHistory h = history.get(i);
                Map<String, String> msg = new HashMap<>();
                msg.put("role", h.getRole());
                msg.put("content", h.getMessage());
                messages.add(msg);
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.8);
            requestBody.put("max_tokens", purpose.equals("chat") ? 150 : 300);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(GROQ_API_KEY);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
            
        } catch (Exception e) {
            e.printStackTrace();
            return getFallbackResponse(userMessage);
        }
    }

    private String getSystemPrompt(String mode, String level, String purpose, List<String> targetWords) {
        String targetVocabInstruction = "";
        if (targetWords != null && !targetWords.isEmpty()) {
            targetVocabInstruction = "\n🎯 TARGET VOCABULARY TO TEACH: " + String.join(", ", targetWords) + 
                "\nTry to naturally use 1-2 of these words in your response so the user can learn them. " +
                "If the user uses them correctly, praise them. If they use any words incorrectly, gently correct them.";
        }

        if (purpose.equals("chat")) {
            if (mode.equals("practice") || mode.equals("voice")) {
                return """
                    You are a friendly English conversation partner.
                    You are having a normal, natural conversation with someone who wants to practice English.
                    
                    RULES:
                    1. Keep responses SHORT (1-2 sentences, 10-20 words)
                    2. Be conversational and friendly
                    3. You can talk about ANY topic the user brings up
                    4. Ask follow-up questions to keep conversation flowing
                    5. DO NOT give lengthy explanations
                    %s
                    
                    User level: %s
                    """.formatted(targetVocabInstruction, level);
            } else if (mode.equals("guided")) {
                return """
                    You are an English tutor. Help user improve their English naturally.
                    Keep responses short. Correct grammar errors gently.
                    Ask follow-up questions. %s
                    User level: %s
                    """.formatted(targetVocabInstruction, level);
            } else if (mode.equals("challenge")) {
                return """
                    You are a strict HR interviewer. Ask professional interview questions.
                    Keep responses short. Be strict but fair. %s
                    User level: %s
                    """.formatted(targetVocabInstruction, level);
            }
        }
        return "You are a helpful assistant. Answer briefly and naturally.";
    }

    private String extractBetween(String text, String start, String end) {
        int startIdx = text.indexOf(start) + start.length();
        int endIdx = text.indexOf(end);
        if (startIdx > start.length() - 1 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx).trim();
        }
        return "";
    }

    private String getFallbackResponse(String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) return "Hi there! How can I help you practice English today?";
        if (lower.contains("school") || lower.contains("study") || lower.contains("learn")) return "Oh, studying is so exciting! What subject do you like most, or what are you learning?";
        if (lower.contains("hobby") || lower.contains("hobbies") || lower.contains("free time")) return "Hobbies are a great way to unwind! What do you enjoy doing in your free time?";
        if (lower.contains("weather") || lower.contains("rain") || lower.contains("sunny") || lower.contains("hot") || lower.contains("cold")) return "The weather really affects our mood, doesn't it? How is the weather where you are right now?";
        if (lower.contains("music") || lower.contains("song") || lower.contains("band")) return "Music is a universal language! What genre or artist do you listen to the most?";
        if (lower.contains("movie") || lower.contains("film") || lower.contains("series") || lower.contains("netflix")) return "I love movies! Do you prefer action, horror, comedy, or romance?";
        if (lower.contains("sport") || lower.contains("football") || lower.contains("soccer") || lower.contains("basketball") || lower.contains("run")) return "Staying active is very important! Do you play any sports or exercise regularly?";
        if (lower.contains("food") || lower.contains("eat") || lower.contains("dinner") || lower.contains("lunch") || lower.contains("cook")) return "Yum! Trying new foods is always exciting. What is your absolute favorite dish?";
        if (lower.contains("work") || lower.contains("job") || lower.contains("office") || lower.contains("career")) return "Career growth is quite a journey! What field do you work in, or what is your dream job?";
        if (lower.contains("family") || lower.contains("parent") || lower.contains("sibling") || lower.contains("brother") || lower.contains("sister")) return "Family is everything. Do you have a large family, or any siblings?";
        if (lower.contains("animal") || lower.contains("pet") || lower.contains("dog") || lower.contains("cat")) return "Pets bring so much joy! Do you have any pets, or are you a cat/dog person?";
        if (lower.contains("travel") || lower.contains("trip") || lower.contains("holiday") || lower.contains("vacation")) return "Traveling expands our horizons! What is the most memorable place you have ever visited?";
        if (lower.contains("yes") || lower.contains("yeah") || lower.contains("yup")) return "I see! Can you share a bit more detail about that?";
        if (lower.contains("no") || lower.contains("nay") || lower.contains("nope")) return "No worries! What would you like to talk about instead?";
        if (lower.contains("thank")) return "You're very welcome! Keep up the great work!";
        return "That's very interesting! Can you tell me a little more about that?";
    }

    private void saveMessage(Long userId, String sessionId, String role, String message) {
        ConversationHistory history = new ConversationHistory();
        history.setUserId(userId);
        history.setSessionId(sessionId);
        history.setRole(role);
        history.setMessage(message);
        history.setCreatedAt(LocalDateTime.now());
        historyRepo.save(history);
    }

    private String[] generateTopicSuggestions(String mode, String aiResponse, List<ConversationHistory> history) {
        if (!mode.equals("practice")) {
            return new String[0];
        }

        String lastTopics = "";
        int count = 0;
        for (int i = history.size() - 1; i >= 0 && count < 5; i--) {
            ConversationHistory h = history.get(i);
            if (h.getRole().equals("user")) {
                lastTopics += h.getMessage() + " ";
                count++;
            }
        }

        String[] suggestions = new String[5];
        suggestions[0] = "Talk about your hobbies";
        suggestions[1] = "Describe your daily routine";
        suggestions[2] = "Share your favorite food";
        suggestions[3] = "Ask me anything random!";
        suggestions[4] = "Choose your own topic";

        if (lastTopics.toLowerCase().contains("food") || lastTopics.toLowerCase().contains("eat")) {
            suggestions[0] = "Tell me about cooking";
            suggestions[1] = "Your favorite restaurant";
        } else if (lastTopics.toLowerCase().contains("work") || lastTopics.toLowerCase().contains("job")) {
            suggestions[0] = "Describe your job";
            suggestions[1] = "Work-life balance";
        } else if (lastTopics.toLowerCase().contains("travel") || lastTopics.toLowerCase().contains("trip")) {
            suggestions[0] = "Your dream vacation";
            suggestions[1] = "Travel experiences";
        }

        return suggestions;
    }

    private boolean isGroqKeyMissing() {
        return GROQ_API_KEY == null || GROQ_API_KEY.trim().isEmpty() || GROQ_API_KEY.startsWith("${");
    }

    private Map<String, Object> checkGrammarRuleBased(String text) {
        Map<String, Object> result = new HashMap<>();
        int grammarScore = 0;
        int clarityScore = 0;
        int confidenceScore = 0;
        List<String> errors = new ArrayList<>();
        
        String clean = text.trim();
        String corrected = clean;
        
        // 1. Capitalization
        if (clean.length() > 0 && !Character.isUpperCase(clean.charAt(0))) {
            errors.add("Kalimat harus diawali dengan huruf kapital");
            corrected = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        }
        
        // 2. Punctuation
        if (clean.length() > 0 && !clean.endsWith(".") && !clean.endsWith("?") && !clean.endsWith("!")) {
            errors.add("Gunakan tanda baca di akhir kalimat");
            corrected = corrected + ".";
        }
        
        // 3. Double words
        java.util.regex.Pattern doubleWordPat = java.util.regex.Pattern.compile("\\b(\\w+)\\s+\\1\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher doubleWordMat = doubleWordPat.matcher(corrected);
        while (doubleWordMat.find()) {
            String word = doubleWordMat.group(1);
            errors.add("Ditemukan pengulangan kata '" + word + "'");
            corrected = corrected.replaceAll("(?i)\\b" + word + "\\s+" + word + "\\b", word);
        }
        
        // 4. "a" before vowel
        java.util.regex.Pattern aVowelPat = java.util.regex.Pattern.compile("\\ba\\s+([aeiouAEIOU]\\w*)\\b");
        java.util.regex.Matcher aVowelMat = aVowelPat.matcher(corrected);
        boolean hasAVowel = false;
        while (aVowelMat.find()) {
            grammarScore -= 2;
            hasAVowel = true;
        }
        if (hasAVowel) {
            errors.add("Gunakan 'an' sebelum kata berawalan huruf vokal");
            corrected = corrected.replaceAll("\\ba\\s+([aeiouAEIOU]\\w*)\\b", "an $1");
        }
        
        // 5. "an" before consonant
        java.util.regex.Pattern anConsPat = java.util.regex.Pattern.compile("\\ban\\s+([bcdfghjklmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ]\\w*)\\b");
        java.util.regex.Matcher anConsMat = anConsPat.matcher(corrected);
        boolean hasAnCons = false;
        while (anConsMat.find()) {
            String word = anConsMat.group(1).toLowerCase();
            if (!word.startsWith("hour") && !word.startsWith("honest") && !word.startsWith("honor")) {
                grammarScore -= 2;
                hasAnCons = true;
            }
        }
        if (hasAnCons) {
            errors.add("Gunakan 'a' sebelum kata berawalan huruf konsonan");
            java.util.regex.Matcher m = anConsPat.matcher(corrected);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String word = m.group(1).toLowerCase();
                if (!word.startsWith("hour") && !word.startsWith("honest") && !word.startsWith("honor")) {
                    m.appendReplacement(sb, "a " + m.group(1));
                } else {
                    m.appendReplacement(sb, m.group(0));
                }
            }
            m.appendTail(sb);
            corrected = sb.toString();
        }
        
        // 6. Subject-Verb agreement (he/she/it)
        String[] thirdPersonBases = {"have", "go", "do", "like", "want", "play", "study", "work", "say", "tell", "speak", "make", "take"};
        String[] thirdPersonSingulars = {"has", "goes", "does", "likes", "wants", "plays", "studies", "works", "says", "tells", "speaks", "makes", "takes"};
        for (int i = 0; i < thirdPersonBases.length; i++) {
            java.util.regex.Pattern svPat = java.util.regex.Pattern.compile("\\b(he|she|it)\\s+" + thirdPersonBases[i] + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
            if (svPat.matcher(corrected).find()) {
                grammarScore -= 2;
                errors.add("Gunakan verb bentuk ketiga tunggal (" + thirdPersonSingulars[i] + ") untuk subjek he/she/it");
                corrected = corrected.replaceAll("(?i)\\b(he|she|it)\\s+" + thirdPersonBases[i] + "\\b", "$1 " + thirdPersonSingulars[i]);
            }
        }
        
        // 7. Subject-Verb agreement (i/you/we/they)
        for (int i = 0; i < thirdPersonSingulars.length; i++) {
            java.util.regex.Pattern svPat = java.util.regex.Pattern.compile("\\b(i|you|we|they)\\s+" + thirdPersonSingulars[i] + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
            if (svPat.matcher(corrected).find()) {
                grammarScore -= 2;
                errors.add("Gunakan verb dasar (" + thirdPersonBases[i] + ") untuk subjek I/you/we/they");
                corrected = corrected.replaceAll("(?i)\\b(i|you|we|they)\\s+" + thirdPersonSingulars[i] + "\\b", "$1 " + thirdPersonBases[i]);
            }
        }
        
        // 8. Subject-to-be agreement
        if (corrected.matches("(?i).*\\b(i)\\s+(is|are)\\b.*")) {
            grammarScore -= 2;
            errors.add("Gunakan 'am' untuk subjek I");
            corrected = corrected.replaceAll("(?i)\\b(i)\\s+(is|are)\\b", "$1 am");
        }
        if (corrected.matches("(?i).*\\b(he|she|it)\\s+(am|are)\\b.*")) {
            grammarScore -= 2;
            errors.add("Gunakan 'is' untuk subjek he/she/it");
            corrected = corrected.replaceAll("(?i)\\b(he|she|it)\\s+(am|are)\\b", "$1 is");
        }
        if (corrected.matches("(?i).*\\b(you|we|they)\\s+(am|is)\\b.*")) {
            grammarScore -= 2;
            errors.add("Gunakan 'are' untuk subjek you/we/they");
            corrected = corrected.replaceAll("(?i)\\b(you|we|they)\\s+(am|is)\\b", "$1 are");
        }
        
        // 9. Hesitation
        String[] hesitations = {"maybe", "think", "uhm", "uh", "sorry", "guess"};
        for (String hes : hesitations) {
            java.util.regex.Pattern hesPat = java.util.regex.Pattern.compile("\\b" + hes + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher hesMat = hesPat.matcher(corrected);
            while (hesMat.find()) {
                confidenceScore -= 1;
            }
        }
        
        grammarScore = Math.max(3, grammarScore);
        clarityScore = Math.max(3, clarityScore);
        confidenceScore = Math.max(3, confidenceScore);
        
        String feedback;
        if (errors.isEmpty()) {
            feedback = "⚠️ [Mode Offline / API Key Groq Belum Diatur] Analisis tata bahasa AI tidak aktif. Secara kasat mata, struktur dasar kalimat Anda terlihat bisa dipahami.";
            grammarScore = 5;
            clarityScore = 5;
            confidenceScore = 5;
        } else {
            String errorSummary = String.join(", ", errors);
            feedback = "⚠️ [Mode Offline] Koreksi Dasar: " + errorSummary + ". Saran: \"" + corrected + "\".";
            grammarScore = Math.max(0, 5 - (errors.size() * 2));
            clarityScore = Math.max(0, 5 - errors.size());
            confidenceScore = 5;
        }
        
        result.put("grammarScore", grammarScore);
        result.put("clarityScore", clarityScore);
        result.put("confidenceScore", confidenceScore);
        result.put("feedback", feedback);
        return result;
    }

    private Map<String, String> generateStyleAnswersRuleBased(String text) {
        Map<String, String> result = new HashMap<>();
        
        Map<String, Object> grammarCheck = checkGrammarRuleBased(text);
        String feedback = (String) grammarCheck.get("feedback");
        String natural = text;
        if (feedback.contains("Versi yang benar: \"")) {
            int start = feedback.indexOf("Versi yang benar: \"") + 18;
            int end = feedback.indexOf("\"", start);
            if (end > start) {
                natural = feedback.substring(start, end);
            }
        }
        result.put("natural", natural);
        
        String ielts = natural;
        Map<String, String> academicSynonyms = new HashMap<>();
        academicSynonyms.put("very", "significantly");
        academicSynonyms.put("good", "exceptional");
        academicSynonyms.put("bad", "detrimental");
        academicSynonyms.put("happy", "gratified");
        academicSynonyms.put("sad", "melancholy");
        academicSynonyms.put("think", "postulate");
        academicSynonyms.put("want", "desire");
        academicSynonyms.put("like", "appreciate");
        academicSynonyms.put("make", "construct");
        academicSynonyms.put("do", "execute");
        academicSynonyms.put("help", "assist");
        academicSynonyms.put("job", "occupation");
        academicSynonyms.put("problem", "complication");
        academicSynonyms.put("change", "modify");
        
        for (Map.Entry<String, String> entry : academicSynonyms.entrySet()) {
            ielts = ielts.replaceAll("(?i)\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        ielts = ielts.replaceAll("(?i)\\ba\\s+([aeiouAEIOU])", "an $1");
        
        String[] ieltsPrefixes = {
            "It is highly arguable that ",
            "From an academic perspective, ",
            "It is of paramount importance to note that "
        };
        ielts = ieltsPrefixes[Math.abs(text.hashCode()) % ieltsPrefixes.length] + Character.toLowerCase(ielts.charAt(0)) + ielts.substring(1);
        result.put("ielts", ielts);
        
        String slang = natural;
        Map<String, String> slangSynonyms = new HashMap<>();
        slangSynonyms.put("going to", "gonna");
        slangSynonyms.put("want to", "wanna");
        slangSynonyms.put("got to", "gotta");
        slangSynonyms.put("you", "ya");
        slangSynonyms.put("hello", "hey");
        slangSynonyms.put("yes", "yeah");
        slangSynonyms.put("really", "deadass");
        slangSynonyms.put("great", "fire");
        slangSynonyms.put("friend", "homie");
        
        for (Map.Entry<String, String> entry : slangSynonyms.entrySet()) {
            slang = slang.replaceAll("(?i)\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        
        String[] slangPrefixes = {"Yo, ", "Basically, ", "Honestly, "};
        String[] slangSuffixes = {", for real", ", no cap", ", you know?"};
        int code = Math.abs(text.hashCode());
        slang = slangPrefixes[code % slangPrefixes.length] + Character.toLowerCase(slang.charAt(0)) + slang.substring(1);
        if (slang.endsWith(".")) slang = slang.substring(0, slang.length() - 1);
        slang = slang + slangSuffixes[code % slangSuffixes.length] + ".";
        
        result.put("slang", slang);
        return result;
    }

    private String callMyMemoryTranslation(String text) {
        try {
            String url = "https://api.mymemory.translated.net/get?q=" + 
                         java.net.URLEncoder.encode(text, "UTF-8") + 
                         "&langpair=en|id";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String translated = root.path("responseData").path("translatedText").asText();
            // URL-decode the response (MyMemory sometimes returns encoded chars like %2C)
            try {
                translated = java.net.URLDecoder.decode(translated, "UTF-8");
            } catch (Exception decodeErr) {
                // ignore decode errors, use raw response
            }
            return translated;
        } catch (Exception e) {
            System.err.println("MyMemory Translation failed: " + e.getMessage());
            return "Gagal menerjemahkan secara otomatis (Layanan offline).";
        }
    }


    private String translateMessageFallback(String message, String type, Long userId) {
        try {
            if ("smart".equals(type)) {
                List<com.speaknow.model.WordEntry> userWords = dictionaryService.getUserWords(userId);
                java.util.Set<String> knownWords = new java.util.HashSet<>();
                for (com.speaknow.model.WordEntry w : userWords) {
                    knownWords.add(w.getWord().toLowerCase());
                }
                
                String[] words = message.toLowerCase().split("\\s+");
                java.util.Set<String> unknownWords = new java.util.HashSet<>();
                for(String w : words) {
                    w = w.replaceAll("[^a-z]", "");
                    if(w.length() > 2 && !dictionaryService.isStopWord(w) && !knownWords.contains(w)) {
                        unknownWords.add(w);
                    }
                }
                
                if (unknownWords.isEmpty()) {
                    return "✅ Kosakata sudah familier bagi Anda (tidak ada kata sulit/baru).";
                }
                
                StringBuilder sb = new StringBuilder();
                sb.append("Layanan Penerjemah Cerdas (Offline Fallback):\n");
                for (String word : unknownWords) {
                    String translation = callMyMemoryTranslation(word);
                    sb.append("- ").append(word).append(": ").append(translation).append("\n");
                }
                return sb.toString();
            } else {
                return callMyMemoryTranslation(message);
            }
        } catch (Exception e) {
            return "Maaf, terjadi kesalahan saat menerjemahkan.";
        }
    }
}
