package com.akrios.rag.Service.Core;

import com.akrios.rag.Config.DocumentLoaderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class VectorStoreService {

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    private final VectorStore vectorStore; // Chroma backend
    private final EmbeddingModel embeddingModel;
    private final DocumentLoaderService loaderService;

    // In-memory fallback
    private final List<Document> inMemoryDocs = new ArrayList<>();
    private final List<double[]> inMemoryEmbeddings = new ArrayList<>();

    public VectorStoreService(VectorStore vectorStore,
                              EmbeddingModel embeddingModel,
                              DocumentLoaderService loaderService, DocumentLoaderConfig config) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.loaderService = loaderService;
    }

    /**
     * Initialize vector store: load, chunk, and embed
     */
    public void initialize() {
        log.info("Starting vector store initialization...");

        List<Document> docs = loaderService.loadDocuments();
        log.info("Loaded {} raw documents from loader service.", docs.size());

        List<Document> chunkedDocs = chunkDocuments(docs);
        log.info("Created {} document chunks.", chunkedDocs.size());

        // Add to vector store
        try {
            addInBatches(chunkedDocs, 300);
            log.info("Added chunks to backend vector store successfully.");
        } catch (Exception e) {
            log.warn("Failed to add chunks to vector store. Falling back to in-memory only. Error: {}", e.getMessage());
        }

        log.info("Vector store initialization completed.");
    }

    /**
     * Search documents via vector store or in-memory fallback
     */
    public List<Document> search(String query, int topK) {
        log.info("Searching for query: \"{}\" with topK={}", query, topK);

        float[] queryArray = embeddingModel.embed(query);
        double[] queryVec = new double[queryArray.length];
        for (int i = 0; i < queryArray.length; i++) queryVec[i] = queryArray[i];

        PriorityQueue<Map.Entry<Document, Double>> pq =
                new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (int i = 0; i < inMemoryDocs.size(); i++) {
            double sim = cosineSimilarity(queryVec, inMemoryEmbeddings.get(i));
            pq.offer(new AbstractMap.SimpleEntry<>(inMemoryDocs.get(i), sim));
            if (pq.size() > topK) pq.poll();
        }

        List<Document> results = new ArrayList<>();
        while (!pq.isEmpty()) results.add(pq.poll().getKey());
        Collections.reverse(results);

        log.info("Search completed. Returning " + results.size() + " documents.");
        return results;
    }

    private void addInBatches(List<Document> docs, int batchSize) {
        for (int i = 0; i < docs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, docs.size());
            List<Document> batch = docs.subList(i, end);
            log.info("Ingesting batch " + (i / batchSize + 1) + "/" + (docs.size() / batchSize + 1) + " (size=" + batch.size() + ")");
            vectorStore.add(batch);
        }
    }

    public List<Document> chunkDocuments(List<Document> docs) {
        log.info("🚀 Starting document chunking with size=" + DEFAULT_CHUNK_SIZE +
                " and overlap=" + DEFAULT_CHUNK_OVERLAP);

        List<Document> chunks = new ArrayList<>();
        int docIndex = 0;

        for (Document doc : docs) {
            String text = sanitizeText(doc.getText());
            if (text.isBlank()) {
                log.warn("Skipping empty or invalid document at index {}", docIndex);
                continue;
            }

            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + DEFAULT_CHUNK_SIZE, text.length());
                String chunkText = text.substring(start, end);

                // ✅ Final safety check: truncate any huge chunk
                if (chunkText.length() > MAX_SAFE_CHUNK_SIZE) {
                    log.warn("Chunk too large ({} chars). Truncating to {} chars.",
                            chunkText.length(), MAX_SAFE_CHUNK_SIZE);
                    chunkText = chunkText.substring(0, MAX_SAFE_CHUNK_SIZE);
                }

                chunks.add(Document.builder()
                        .text(chunkText)
                        .metadata(doc.getMetadata())
                        .build());

                log.info("Created chunk [{}-{}] ({} chars)", start, end, chunkText.length());
                start += DEFAULT_CHUNK_SIZE - DEFAULT_CHUNK_OVERLAP;
            }

            docIndex++;
        }

        log.info("✅ Finished chunking. Created {} chunks total.", chunks.size());
        return chunks;
    }

    /**
     * Sanitize text to remove binary characters, control chars, and invalid unicode
     */
    private String sanitizeText(String text) {
        if (text == null) return "";
        // Remove non-printable chars and control chars
        String cleaned = text.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", ""); // ASCII printable + \n\r\t
        // Also remove any leftover unicode control chars
        cleaned = cleaned.replaceAll("\\p{C}", "");
        return cleaned.trim();
    }

    // You can tune this — 8000 is a safe choice for most embedding models
    private static final int MAX_SAFE_CHUNK_SIZE = 8000;

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }

    public void initializeFromScratch(List<Document> chunkedDocs) {
        log.info("🧠 Adding chunks to vector store...");
        vectorStore.add(chunkedDocs);
        cacheInMemory(chunkedDocs);
        log.info("✅ Vector store initialized from scratch.");
    }

    public void initializeFromCached(List<Document> cachedDocs) {
        log.info("🔁 Rehydrating vector store from cached embeddings...");
        vectorStore.add(cachedDocs);
        cacheInMemory(cachedDocs);
        log.info("✅ Vector store restored from cache.");
    }

    private void cacheInMemory(List<Document> chunkedDocs) {
        inMemoryDocs.clear();
        inMemoryEmbeddings.clear();
        for (Document doc : chunkedDocs) {
            float[] embArray = embeddingModel.embed(Objects.requireNonNull(doc.getText()));
            double[] emb = new double[embArray.length];
            for (int i = 0; i < embArray.length; i++) emb[i] = embArray[i];
            inMemoryEmbeddings.add(emb);
            inMemoryDocs.add(doc);
        }
    }
}
