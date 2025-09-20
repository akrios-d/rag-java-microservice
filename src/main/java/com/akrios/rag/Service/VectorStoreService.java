package com.akrios.rag.Service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

@Service
public class VectorStoreService {

    private static final Logger logger = Logger.getLogger(VectorStoreService.class.getName());

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    private final EmbeddingModel embeddingModel;
    private final DocumentLoaderService loaderService;

    // Store chunked documents and embeddings for simple retrieval
    private final List<Document> documents = new ArrayList<>();
    private final List<double[]> embeddings = new ArrayList<>();

    public VectorStoreService(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
                              DocumentLoaderService loaderService) {
        this.embeddingModel = embeddingModel;
        this.loaderService = loaderService;
    }

    /**
     * Initialize vector store:
     * - Load documents
     * - Chunk documents
     * - Generate embeddings for search
     */
    public void initialize() {
        logger.info("Initializing vector store...");

        List<Document> docs = loaderService.loadDocuments();
        logger.info("Loaded " + docs.size() + " documents.");

        List<Document> chunkedDocs = chunkDocuments(docs);
        logger.info("Total chunks created: " + chunkedDocs.size());

        // Generate embeddings
        for (Document doc : chunkedDocs) {
            documents.add(doc);

            // Spring AI embed returns float[]
            float[] embArray = embeddingModel.embed(doc.getText());
            double[] emb = new double[embArray.length];
            for (int i = 0; i < embArray.length; i++) {
                emb[i] = embArray[i];
            }
            embeddings.add(emb);
        }

        logger.info("Vector store initialized successfully.");
    }

    /**
     * Chunk documents into smaller pieces with overlap
     */
    private List<Document> chunkDocuments(List<Document> docs) {
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
        return chunks;
    }

    /**
     * Simple retrieval based on cosine similarity
     */
    public List<Document> search(String query, int topK) {
        float[] queryArray = embeddingModel.embed(query);
        double[] queryVec = new double[queryArray.length];
        for (int i = 0; i < queryArray.length; i++) {
            queryVec[i] = queryArray[i];
        }

        PriorityQueue<Map.Entry<Document, Double>> pq = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (int i = 0; i < documents.size(); i++) {
            double sim = cosineSimilarity(queryVec, embeddings.get(i));
            pq.offer(new AbstractMap.SimpleEntry<>(documents.get(i), sim));
            if (pq.size() > topK) pq.poll();
        }

        List<Document> results = new ArrayList<>();
        while (!pq.isEmpty()) results.add(pq.poll().getKey());
        Collections.reverse(results);
        return results;
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
