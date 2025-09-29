package com.akrios.rag.Service.Core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class VectorStoreService {

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;
    private static final int MAX_SAFE_CHUNK_SIZE = 8000;
    private static final int BATCH_SIZE = 100;

    private final VectorStore vectorStore; // PGVector backend
    private final DocumentLoaderService loaderService;

    public VectorStoreService(VectorStore vectorStore,
                              EmbeddingModel embeddingModel,
                              DocumentLoaderService loaderService) {
        this.vectorStore = vectorStore;
        this.loaderService = loaderService;
    }

    /**
     * Initialize vector store: load, chunk, embed, and persist in PostgreSQL
     */
    public void initialize() {
        log.info("Starting vector store initialization...");

        try {
            List<Document> rawDocs = loaderService.loadDocuments();
            log.info("Loaded {} raw documents from loader.", rawDocs.size());

            List<Document> chunkedDocs = chunkDocuments(rawDocs);
            log.info("Chunked documents into {} chunks.", chunkedDocs.size());

            batchAddToVectorStore(chunkedDocs);
            log.info("Vector store initialization completed successfully.");

        } catch (Exception e) {
            log.error("Failed to initialize vector store: {}", e.getMessage(), e);
        }
    }

    List<Document> chunkDocuments(List<Document> docs) {
        log.info("Starting document chunking with size={} and overlap={}", DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
        List<Document> chunks = new ArrayList<>();

        for (Document doc : docs) {
            String text = sanitizeText(doc.getText());
            if (text.isBlank()) {
                log.warn("Skipping empty/invalid document with metadata {}", doc.getMetadata());
                continue;
            }

            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + DEFAULT_CHUNK_SIZE, text.length());
                String chunkText = text.substring(start, end);
                if (chunkText.length() > MAX_SAFE_CHUNK_SIZE) {
                    log.warn("Chunk too large ({}). Truncating to {}", chunkText.length(), MAX_SAFE_CHUNK_SIZE);
                    chunkText = chunkText.substring(0, MAX_SAFE_CHUNK_SIZE);
                }

                Document chunk = Document.builder()
                        .text(chunkText)
                        .metadata(doc.getMetadata())
                        .build();
                chunks.add(chunk);

                start += DEFAULT_CHUNK_SIZE - DEFAULT_CHUNK_OVERLAP;
            }
        }

        log.info("Finished chunking. Created {} chunks.", chunks.size());
        return chunks;
    }

    private void batchAddToVectorStore(List<Document> chunkedDocs) {
        int total = chunkedDocs.size();
        for (int i = 0; i < total; i += VectorStoreService.BATCH_SIZE) {
            int end = Math.min(i + VectorStoreService.BATCH_SIZE, total);
            List<Document> batch = chunkedDocs.subList(i, end);
            log.info("Adding batch {}/{} (size={})", i / VectorStoreService.BATCH_SIZE + 1, (total + VectorStoreService.BATCH_SIZE - 1) / VectorStoreService.BATCH_SIZE, batch.size());
            try {
                vectorStore.add(batch);
            } catch (Exception e) {
                log.error("Failed to add batch to vector store: {}", e.getMessage(), e);
            }
        }
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", "");
        cleaned = cleaned.replaceAll("\\p{C}", "");
        return cleaned.trim();
    }

    /**
     * Search using PGVector backend and return documents
     */
    public List<Document> search(String query, int topK) {
        log.info("Searching query: '{}' topK={}", query, topK);
        try {
            return vectorStore.similaritySearch(new SearchRequest() )
            return vectorStore.similaritySearch(query);
        } catch (Exception e) {
            log.error("Failed to search vector store: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
