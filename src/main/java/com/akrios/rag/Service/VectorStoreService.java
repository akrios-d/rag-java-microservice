package com.akrios.rag.Service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class VectorStoreService {

    private static final Logger logger = Logger.getLogger(VectorStoreService.class.getName());

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    private final VectorStore vectorStore;
    private final DocumentLoaderService loaderService;

    public VectorStoreService(VectorStore vectorStore,
                              @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
                              DocumentLoaderService loaderService) {
        this.vectorStore = vectorStore;
        this.loaderService = loaderService;
    }

    /**
     * Initialize the vector store:
     * - Load documents
     * - Chunk them
     * - Generate embeddings and add to vector store
     */
    public void initialize() {
        logger.info("Initializing vector store...");

        List<Document> documents = loaderService.loadDocuments();
        logger.info("Loaded " + documents.size() + " documents.");

        List<Document> chunkedDocs = chunkDocuments(documents);
        logger.info("Total chunks created: " + chunkedDocs.size());

        vectorStore.add(chunkedDocs);

        logger.info("Vector store initialized successfully.");
    }

    /**
     * Chunk documents into smaller pieces with overlap
     */
    private List<Document> chunkDocuments(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();

        for (Document doc : documents) {
            String text = doc.getText();
            int start = 0;

            while (start < text.length()) {
                int end = Math.min(start + DEFAULT_CHUNK_SIZE, text.length());
                String chunkText = text.substring(start, end);

                chunks.add(Document.builder()
                        .text(chunkText)
                        .metadata(doc.getMetadata())
                        .build());

                start += (DEFAULT_CHUNK_SIZE - DEFAULT_CHUNK_OVERLAP);
            }
        }

        return chunks;
    }
}
