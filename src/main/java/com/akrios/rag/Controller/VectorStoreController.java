package com.akrios.rag.Controller;

import com.akrios.rag.Service.Core.VectorStoreService;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/vectorstore")
public class VectorStoreController {

    private static final Logger logger = Logger.getLogger(VectorStoreController.class.getName());

    private final VectorStoreService vectorStoreService;

    public VectorStoreController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping("/initialize")
    public String initializeVectorStore() {
        logger.info("Received request to initialize vector store...");
        try {
            vectorStoreService.initialize();
            return "Vector store initialized successfully!";
        } catch (Exception e) {
            logger.severe("Error initializing vector store: " + e.getMessage());
            return "Error initializing vector store: " + e.getMessage();
        }
    }

    /**
     * Optional: trigger re-indexing if needed
     */
    @PostMapping("/reindex")
    public String reindexVectorStore() {
        logger.info("Reindexing vector store...");
        try {
            vectorStoreService.initialize();
            return "Vector store reindexed successfully!";
        } catch (Exception e) {
            logger.severe("Error reindexing vector store: " + e.getMessage());
            return "Error reindexing vector store: " + e.getMessage();
        }
    }
}
