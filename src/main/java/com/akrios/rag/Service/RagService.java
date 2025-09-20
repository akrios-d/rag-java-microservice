package com.akrios.rag.Service;

import com.akrios.rag.Service.Core.ConversationMemoryService;
import com.akrios.rag.Service.Core.MultiQueryRetriever;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import static com.akrios.rag.Prompts.PromptTemplates.RAG_PROMPT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final OllamaChatModel ollama;
    private final MultiQueryRetriever retriever;
    private final ConversationMemoryService memory;

    public RagService(OllamaChatModel ollama, MultiQueryRetriever retriever, ConversationMemoryService memory) {
        this.ollama = ollama;
        this.retriever = retriever;
        this.memory = memory;
    }

    public String ask(String userId, String question, boolean useMultiQuery) {
        logger.info("Received question from user [{}]: {}", userId, question);
        logger.debug("Multi-query mode: {}", useMultiQuery);

        // Step 1: Retrieve documents
        List<Document> docs = retriever.retrieve(question, useMultiQuery);
        logger.info("Retrieved {} documents from vector store", docs.size());

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        logger.debug("Constructed context: {}", context);

        // Step 2: Build prompt
        Map<String, Object> vars = Map.of(
                "context", context,
                "history", memory.getHistory(userId),
                "question", question
        );

        String prompt = RAG_PROMPT
                .replace("{context}", (String) vars.get("context"))
                .replace("{history}", (String) vars.get("history"))
                .replace("{question}", (String) vars.get("question"));

        logger.debug("Final prompt sent to Ollama:\n{}", prompt);

        // Step 3: Get answer from Ollama
        String answer = ollama.call(prompt);
        logger.info("Generated answer length: {}", answer != null ? answer.length() : 0);

        // Step 4: Save to conversation memory
        memory.append(userId, "User", question);
        memory.append(userId, "AI", answer);
        logger.info("Conversation updated for user [{}]", userId);

        return answer;
    }
}
