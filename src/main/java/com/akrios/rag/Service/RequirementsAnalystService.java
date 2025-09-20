package com.akrios.rag.Service;

import com.akrios.rag.Prompts.PromptTemplates;
import com.akrios.rag.Service.Core.DocWriterService;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RequirementsAnalystService {

    private static final Logger logger = Logger.getLogger(RequirementsAnalystService.class.getName());

    private final OllamaChatModel chatModel;
    private final DocWriterService docWriterService;

    // Store user interactions in memory
    private final Map<String, List<String>> chatMemory = new ConcurrentHashMap<>();

    public RequirementsAnalystService(OllamaChatModel chatModel, DocWriterService docWriterService) {
        this.chatModel = chatModel;
        this.docWriterService = docWriterService;
    }

    /**
     * Handle user interaction with the analyst agent
     */
    public String interact(String userId, String userMessage) {
        logger.info("User [" + userId + "] message: " + userMessage);

        // Initialize memory for new user
        chatMemory.putIfAbsent(userId, new ArrayList<>());
        List<String> memory = chatMemory.get(userId);
        memory.add("User: " + userMessage);

        // If user types "generate", we finalize
        if ("generate".equalsIgnoreCase(userMessage.trim())) {
            logger.info("User [" + userId + "] requested document generation.");
            String collectedRequirements = memory.stream()
                    .filter(msg -> !msg.equalsIgnoreCase("generate"))
                    .collect(Collectors.joining("\n"));

            // Clear memory for next session
            chatMemory.remove(userId);

            // Call Doc Writer to generate documentation
            String document = docWriterService.generateDocumentation(collectedRequirements);
            logger.info("Documentation generated for user [" + userId + "]");
            return document;
        }

        // Otherwise, generate partial response / follow-up questions
        String prompt = PromptTemplates.SYSTEM_REQUIREMENTS_ANALYST
                + "\n\nConversation so far:\n"
                + String.join("\n", memory)
                + "\n\nUser just said:\n" + userMessage;

        String partialResponse = chatModel.call(prompt);
        logger.info("Partial response generated for user [" + userId + "]");

        memory.add("AI: " + partialResponse);
        return partialResponse;
    }
}
