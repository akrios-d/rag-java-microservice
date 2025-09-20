package com.akrios.rag.Service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConversationMemoryService {
    private final Map<String, StringBuilder> memory = new HashMap<>();

    public String getHistory(String userId) {
        return memory.getOrDefault(userId, new StringBuilder()).toString();
    }

    public void append(String userId, String role, String text) {
        memory.computeIfAbsent(userId, k -> new StringBuilder())
                .append(role).append(": ").append(text).append("\n");
    }
}
