package com.akrios.rag.Service;

import com.akrios.rag.Service.Core.MultiQueryRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import static com.akrios.rag.Prompts.PromptTemplates.RAG_PROMPT;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Slf4j
public class RagService {

    private final OllamaChatModel ollama;
    private final MultiQueryRetriever retriever;
    private final ChatMemory chatMemory;

    public RagService(OllamaChatModel ollama, MultiQueryRetriever retriever, ChatMemory chatMemory) {
        this.ollama = ollama;
        this.retriever = retriever;
        this.chatMemory = chatMemory;
    }

    public String ask(String userId, String question, boolean useMultiQuery) {
        log.info("Received question from user [{}]: {}", userId, question);
        log.debug("Multi-query mode: {}", useMultiQuery);

        // Step 1: Retrieve documents
        List<Document> docs = retriever.retrieve(question, useMultiQuery);
        log.info("Retrieved {} documents from vector store", docs.size());

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        log.debug("Constructed context: {}", context);

        // Step 2: Build prompt history
        List<Message> messages = chatMemory.get(userId);  // returns List<Message>

        String history = messages.stream()
                .map(Message::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));

        String prompt = RAG_PROMPT
                .replace("{context}", context)
                .replace("{history}", history)
                .replace("{question}", question);

        log.debug("Final prompt sent to Ollama:\n{}", prompt);

        // Step 3: Get answer from Ollama
        String answer = ollama.call(prompt);
        log.info("Generated answer length: {}", answer != null ? answer.length() : 0);

        // Step 4: Save messages into ChatMemory
        chatMemory.add(userId, new UserMessage(question));
        chatMemory.add(userId, new AssistantMessage(answer));
        log.info("Chat memory updated for user [{}]", userId);

        return answer;
    }
}
