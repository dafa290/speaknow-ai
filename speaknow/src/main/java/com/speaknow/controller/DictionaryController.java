package com.speaknow.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.speaknow.model.WordEntry;
import com.speaknow.service.DictionaryService;

@RestController
@RequestMapping("/api/dictionary")
@CrossOrigin(origins = "*")
public class DictionaryController {
    
    @Autowired
    private DictionaryService dictionaryService;
    
    @GetMapping("/{userId}")
    public Map<String, Object> getUserDictionary(@PathVariable Long userId) {
        List<WordEntry> words = dictionaryService.getUserWords(userId);
        int mastered = dictionaryService.countMastered(userId);
        
        List<Map<String, Object>> wordList = new ArrayList<>();
        for (WordEntry w : words) {
            Map<String, Object> info = new HashMap<>();
            info.put("word", w.getWord());
            info.put("meaning", w.getMeaning());
            info.put("category", w.getCategory());
            info.put("example", w.getExample());
            info.put("usageCount", w.getUsageCount());
            info.put("mastered", w.isMastered());
            wordList.add(info);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("words", wordList);
        result.put("masteredCount", mastered);
        result.put("totalCount", words.size());
        result.put("progress", words.size() > 0 ? (mastered * 100 / words.size()) : 0);
        return result;
    }
    
    @GetMapping("/{userId}/recommendations")
    public List<Map<String, String>> getRecommendations(@PathVariable Long userId, 
                                                         @RequestParam(defaultValue = "5") int limit) {
        return dictionaryService.getRecommendedWords(userId, limit);
    }
    
    @PostMapping("/{userId}/master")
    public Map<String, Object> markMastered(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        String word = request.get("word");
        WordEntry updated = dictionaryService.markAsMastered(userId, word);
        Map<String, Object> response = new HashMap<>();
        response.put("success", updated != null);
        response.put("word", word);
        return response;
    }
    
    @PostMapping("/{userId}/track")
    public void trackWords(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        String message = request.get("message");
        dictionaryService.trackUsedWords(userId, message);
    }

    @PostMapping("/{userId}/generate-targets")
    public Map<String, Object> generateTargetWords(@PathVariable Long userId) {
        List<WordEntry> newWords = dictionaryService.generateTargetWords(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("addedCount", newWords.size());
        
        List<Map<String, Object>> wordList = new ArrayList<>();
        for (WordEntry w : newWords) {
            Map<String, Object> info = new HashMap<>();
            info.put("word", w.getWord());
            info.put("meaning", w.getMeaning());
            info.put("category", w.getCategory());
            info.put("example", w.getExample());
            info.put("usageCount", w.getUsageCount());
            info.put("mastered", w.isMastered());
            wordList.add(info);
        }
        result.put("words", wordList);
        
        return result;
    }
}