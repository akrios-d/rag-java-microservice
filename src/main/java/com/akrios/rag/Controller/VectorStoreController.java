package com.akrios.rag.Controller;

import com.akrios.rag.Service.Core.ETLInitializerService;
import com.akrios.rag.Service.Core.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/vectorstore")
@Slf4j
public class VectorStoreController {

    private final ETLInitializerService etlInitializerService;

    public VectorStoreController(ETLInitializerService etlInitializerService) {
        this.etlInitializerService = etlInitializerService;
    }


    @PostMapping("/initialize")
    public String initializeVectorStore() {
        log.info("Received request to initialize vector store...");
        try {
            etlInitializerService.initialize();
            return "Vector store initialized successfully!";
        } catch (Exception e) {
            log.error("Error initializing vector store: {}", e.getMessage());
            return "Error initializing vector store: " + e.getMessage();
        }
    }

    /**
     * Optional: trigger re-indexing if needed
     */
    @PostMapping("/reindex")
    public String reindexVectorStore() {
        log.info("Reindexing vector store...");
        try {
            etlInitializerService.initialize();
            return "Vector store reindexed successfully!";
        } catch (Exception e) {
            log.error("Error reindexing vector store: {}", e.getMessage());
            return "Error reindexing vector store: " + e.getMessage();
        }
    }
}
