package com.akrios.rag.Service.Core;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

@Service
public class VectorStoreService {

    private static final Logger logger = Logger.getLogger(VectorStoreService.class.getName());

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
                              DocumentLoaderService loaderService) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.loaderService = loaderService;
    }

    /**
     * Initialize vector store: load, chunk, and embed
     */
    public void initialize() {
        logger.info("Starting vector store initialization...");

        List<Document> docs = loaderService.loadDocuments();
        logger.info("Loaded " + docs.size() + " raw documents from loader service.");

        List<Document> chunkedDocs = chunkDocuments(docs);
        logger.info("Created " + chunkedDocs.size() + " document chunks.");

        // Add to vector store
        try {
            vectorStore.add(chunkedDocs);
            logger.info("Added chunks to backend vector store successfully.");
        } catch (Exception e) {
            logger.warning("Failed to add chunks to vector store. Falling back to in-memory only. Error: " + e.getMessage());
        }

        // Also populate in-memory fallback
        logger.info("Populating in-memory fallback embeddings...");
        for (Document doc : chunkedDocs) {
            float[] embArray = embeddingModel.embed(Objects.requireNonNull(doc.getText()));
            double[] emb = new double[embArray.length];
            for (int i = 0; i < embArray.length; i++) emb[i] = embArray[i];
            inMemoryDocs.add(doc);
            inMemoryEmbeddings.add(emb);
        }
        logger.info("In-memory fallback populated with " + inMemoryDocs.size() + " embeddings.");

        logger.info("Vector store initialization completed.");
    }

    /**
     * Search documents via vector store or in-memory fallback
     */
    public List<Document> search(String query, int topK) {
        logger.info("Searching for query: \"" + query + "\" with topK=" + topK);

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

        logger.info("Search completed. Returning " + results.size() + " documents.");
        return results;
    }

    public List<Document> chunkDocuments(List<Document> docs) {
        logger.info("Starting document chunking with size=" + DEFAULT_CHUNK_SIZE +
                " and overlap=" + DEFAULT_CHUNK_OVERLAP);
        List<Document> chunks = new ArrayList<>();
        for (Document doc : docs) {
            String text = doc.getText();
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + DEFAULT_CHUNK_SIZE, text.length());
                chunks.add(Document.builder()
                        .text(text.substring(start, end))
                        .metadata(doc.getMetadata())
                        .build());
                start += DEFAULT_CHUNK_SIZE - DEFAULT_CHUNK_OVERLAP;
            }
        }
        logger.info("Finished chunking. Created " + chunks.size() + " chunks.");
        return chunks;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }
}
