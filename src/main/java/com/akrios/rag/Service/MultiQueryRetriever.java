package com.akrios.rag.Service;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MultiQueryRetriever {

    private final OllamaChatModel chatModel;
    private final VectorStoreService vectorStore;

    private static final String MULTI_QUERY_SYSTEM_PROMPT = """
        You are an AI language model assistant. Your task is to generate five different versions
        of the given user question to retrieve relevant documents from a vector database.
        By generating multiple perspectives on the user question, your goal is to help the user
        overcome some of the limitations of the distance-based similarity search.
        """;

    public MultiQueryRetriever(OllamaChatModel chatModel, VectorStoreService vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String question, boolean useMultiQuery) {
        if (!useMultiQuery) {
            return vectorStore.search(question, 5);
        }

        // Build the multi-query prompt
        String prompt = MULTI_QUERY_SYSTEM_PROMPT + "\n\nUser Question: " + question;

        // Call Ollama to generate variations
        String expanded = chatModel.call(prompt);

        List<String> variations = Arrays.stream(expanded.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<Document> results = new ArrayList<>();
        for (String q : variations) {
            results.addAll(vectorStore.search(q, 5));
        }

        return results.stream().distinct().collect(Collectors.toList());
    }
}
