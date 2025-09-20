package com.akrios.rag.Service.Core;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.akrios.rag.Prompts.PromptTemplates.MULTI_QUERY_SYSTEM_PROMPT;

@Service
public class MultiQueryRetriever {

    private static final Logger logger = Logger.getLogger(MultiQueryRetriever.class.getName());

    private final OllamaChatModel chatModel;
    private final VectorStoreService vectorStore;

    public MultiQueryRetriever(OllamaChatModel chatModel, VectorStoreService vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String question, boolean useMultiQuery) {
        logger.info("Starting retrieval for question: \"" + question + "\" | MultiQuery=" + useMultiQuery);

        if (!useMultiQuery) {
            logger.info("Using single-query retrieval...");
            List<Document> singleResults = vectorStore.search(question, 5);
            logger.info("Retrieved " + singleResults.size() + " documents for single query.");
            return singleResults;
        }

        // Build the multi-query prompt
        String prompt = MULTI_QUERY_SYSTEM_PROMPT + "\n\nUser Question: " + question;
        logger.info("Generated MultiQuery prompt:\n" + prompt);

        // Call Ollama to generate variations
        String expanded = chatModel.call(prompt);
        logger.info("Ollama expansion raw response:\n" + expanded);

        List<String> variations = Arrays.stream(expanded.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        logger.info("Generated " + variations.size() + " query variations: " + variations);

        List<Document> results = new ArrayList<>();
        for (String q : variations) {
            List<Document> docs = vectorStore.search(q, 5);
            logger.info("Variation: \"" + q + "\" -> Retrieved " + docs.size() + " documents");
            results.addAll(docs);
        }

        List<Document> distinctResults = results.stream().distinct().collect(Collectors.toList());
        logger.info("Final distinct result set contains " + distinctResults.size() + " documents.");

        return distinctResults;
    }
}
