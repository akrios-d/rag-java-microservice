package com.akrios.rag.Service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConversationMemoryService {
    private final Map<String, List<String>> memory = new HashMap<>();

    public void append(String userId, String role, String message) {
        memory.computeIfAbsent(userId, k -> new ArrayList<>())
                .add(role + ": " + message);
    }

    public String getHistory(String userId) {
        return String.join("\n", memory.getOrDefault(userId, List.of()));
    }
}
