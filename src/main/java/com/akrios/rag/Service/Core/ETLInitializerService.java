package com.akrios.rag.Service.Core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

import java.util.List;

@Service
@Slf4j
public class ETLInitializerService {

    private final VectorStoreService vectorStoreService;
    private final DocumentLoaderService documentLoaderService;

    public ETLInitializerService(VectorStoreService vectorStoreService,
                                 DocumentLoaderService documentLoaderService) {
        this.vectorStoreService = vectorStoreService;
        this.documentLoaderService = documentLoaderService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeVectorStore() {
        log.info("🚀 ETLInitializerService starting vector store initialization...");

        try {
            // Load all documents
            List<Document> documents = documentLoaderService.loadDocuments();
            log.info("Loaded {} documents for ETL processing.", documents.size());

            // Initialize vector store (incremental)
            vectorStoreService.initialize();

            log.info("✅ Vector store ETL initialization completed successfully.");
        } catch (Exception e) {
            log.error("❌ ETL initialization failed: {}", e.getMessage(), e);
        }
    }
}
