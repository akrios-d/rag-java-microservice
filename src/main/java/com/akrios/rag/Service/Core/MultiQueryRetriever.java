package com.akrios.rag.Service.Core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.akrios.rag.Prompts.PromptTemplates.MULTI_QUERY_SYSTEM_PROMPT;

@Service
@Slf4j
public class MultiQueryRetriever {

    private final OllamaChatModel chatModel;
    private final VectorStoreService vectorStore;

    public MultiQueryRetriever(OllamaChatModel chatModel, VectorStoreService vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String question, boolean useMultiQuery) {
        log.info("Starting retrieval for question: \"{}\" | MultiQuery={}", question, useMultiQuery);

        if (!useMultiQuery) {
            log.info("Using single-query retrieval...");
            List<Document> singleResults = vectorStore.search(question, 5);
            log.info("Retrieved {} documents for single query.", singleResults.size());
            return singleResults;
        }

        // Build the multi-query prompt
        String prompt = MULTI_QUERY_SYSTEM_PROMPT + "\n\nUser Question: " + question;
        log.info("Generated MultiQuery prompt:\n{}", prompt);

        // Call Ollama to generate variations
        String expanded = chatModel.call(prompt);
        log.info("Ollama expansion raw response:\n{}", expanded);

        List<String> variations = Arrays.stream(expanded.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        log.info("Generated {} query variations: {}", variations.size(), variations);

        List<Document> results = new ArrayList<>();
        for (String q : variations) {
            List<Document> docs = vectorStore.search(q, 5);
            log.info("Variation: \"{}\" -> Retrieved {} documents", q, docs.size());
            results.addAll(docs);
        }

        List<Document> distinctResults = results.stream().distinct().collect(Collectors.toList());
        log.info("Final distinct result set contains {} documents.", distinctResults.size());

        return distinctResults;
    }
}
