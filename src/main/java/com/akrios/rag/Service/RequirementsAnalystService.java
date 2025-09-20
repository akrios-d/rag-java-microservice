package com.akrios.rag.Service;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.akrios.rag.Prompts.PromptTemplates.SYSTEM_REQUIREMENTS_ANALYST;

@Service
public class RequirementsAnalystService {

    private final OllamaChatModel chatModel;

    // Store structured requirements in memory
    private final Map<String, List<Map<String, String>>> sessionRequirements = new HashMap<>();

    public RequirementsAnalystService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public Map<String, Object> interact(String userId, String userMessage) {
        // Step 1: Call analyst agent
        String prompt = SYSTEM_REQUIREMENTS_ANALYST +
                "\n\nConversation so far:\n" +
                getRequirementsText(userId) +
                "\n\nUser: " + userMessage;

        String aiResponse = chatModel.call(prompt);

        // Step 2: Update structured requirements (naïve categorization for demo)
        String category = detectCategory(userMessage);
        sessionRequirements.computeIfAbsent(userId, k -> new ArrayList<>())
                .add(Map.of("category", category, "text", userMessage));

        // Step 3: Build partial draft (based on collected requirements so far)
        String partialDraft = buildPartialDraft(userId);

        // Step 4: Return structured + human-readable
        return Map.of(
                "aiResponse", aiResponse,
                "partialDraft", partialDraft,
                "requirements", sessionRequirements.get(userId)
        );
    }

    public String finalizeRequirements(String userId) {
        List<Map<String, String>> reqs = sessionRequirements.getOrDefault(userId, List.of());
        if (reqs.isEmpty()) return "No requirements collected yet.";

        StringBuilder sb = new StringBuilder("# Requirements Summary\n\n");
        reqs.forEach(r ->
                sb.append("- [").append(r.get("category")).append("] ")
                        .append(r.get("text")).append("\n")
        );
        return sb.toString();
    }

    private String buildPartialDraft(String userId) {
        List<Map<String, String>> reqs = sessionRequirements.getOrDefault(userId, List.of());
        if (reqs.isEmpty()) return "No requirements captured yet.";

        StringBuilder sb = new StringBuilder("📑 Current Draft:\n\n");
        reqs.forEach(r -> {
            switch (r.get("category")) {
                case "Functional" ->
                        sb.append("• The system must ").append(r.get("text")).append("\n");
                case "Non-Functional" ->
                        sb.append("• Performance/Security constraint: ").append(r.get("text")).append("\n");
                case "Constraint" ->
                        sb.append("• Constraint: ").append(r.get("text")).append("\n");
                default ->
                        sb.append("• Note: ").append(r.get("text")).append("\n");
            }
        });
        return sb.toString();
    }

    private String detectCategory(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("must") || lower.contains("should"))
            return "Functional";
        if (lower.contains("performance") || lower.contains("latency") || lower.contains("security"))
            return "Non-Functional";
        if (lower.contains("constraint") || lower.contains("limit"))
            return "Constraint";
        return "General";
    }

    private String getRequirementsText(String userId) {
        return sessionRequirements.getOrDefault(userId, List.of())
                .stream()
                .map(r -> "[" + r.get("category") + "] " + r.get("text"))
                .reduce("", (a, b) -> a + "\n" + b);
    }
}
