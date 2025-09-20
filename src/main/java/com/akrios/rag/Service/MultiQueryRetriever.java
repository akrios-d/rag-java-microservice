package com.akrios.rag.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MultiQueryRetriever {

    private final ChatClient llm;
    private final VectorStore vectorStore;

    public MultiQueryRetriever(ChatClient llm, VectorStore vectorStore) {
        this.llm = llm;
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String question, boolean useMultiQuery) {
        if (!useMultiQuery) {
            return vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(5).build()
            );
        }

        String expansionPrompt = """
            You are an AI assistant. Generate five alternative versions of the following question 
            to improve retrieval from a vector database.

            Original question: %s
            """.formatted(question);

        String expanded = llm.prompt().user(expansionPrompt).call().content();
        List<String> variations = Arrays.stream(expanded.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        return variations.stream()
                .flatMap(q -> vectorStore.similaritySearch(
                        SearchRequest.builder().query(q).topK(3).build()
                ).stream())
                .distinct()
                .collect(Collectors.toList());
    }
}

