package com.akrios.rag.Service;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatClient llm;
    private final MultiQueryRetriever retriever;
    private final ConversationMemoryService memory;

    private static final String RAG_PROMPT = """
        You are an AI assistant tasked with answering questions strictly based on the provided documents.
        Do not use external knowledge or provide answers unrelated to the content retrieved.

        Documents retrieved:
        {context}

        Conversation so far:
        {history}

        User Question: {question}

        Provide a detailed and accurate response based on the documents above.
        """;

    public RagService(ChatClient llm, MultiQueryRetriever retriever, ConversationMemoryService memory) {
        this.llm = llm;
        this.retriever = retriever;
        this.memory = memory;
    }

    public String ask(String userId, String question, boolean useMultiQuery) {
        List<Document> docs = retriever.retrieve(question, useMultiQuery);
        String context = docs.stream()
                .map(Document::getText) // org.springframework.ai.document.Document uses getText()
                .collect(Collectors.joining("\n"));

        Map<String, Object> vars = Map.of(
                "context", context,
                "history", memory.getHistory(userId),
                "question", question
        );

        String prompt = RAG_PROMPT
                .replace("{context}", (String) vars.get("context"))
                .replace("{history}", (String) vars.get("history"))
                .replace("{question}", (String) vars.get("question"));

        String answer = llm.prompt().user(prompt).call().content();

        memory.append(userId, "User", question);
        memory.append(userId, "AI", answer);

        return answer;
    }
}
