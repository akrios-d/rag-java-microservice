package com.akrios.rag.Service.Core;

import com.akrios.rag.Config.DocumentLoaderConfig;
import com.akrios.rag.Service.Core.DocumentsLoader.ConfluenceService;
import com.akrios.rag.Service.Core.DocumentsLoader.LocalFileService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class DocumentLoaderService {

    private static final Logger logger = Logger.getLogger(DocumentLoaderService.class.getName());

    private final LocalFileService localFileService;
    private final ConfluenceService confluenceService;
    private final DocumentLoaderConfig config;

    public DocumentLoaderService(LocalFileService localFileService,
                                 ConfluenceService confluenceService,
                                 DocumentLoaderConfig config) {
        this.localFileService = localFileService;
        this.confluenceService = confluenceService;
        this.config = config;
    }

    public List<Document> loadDocuments() {
        logger.info("=== Document Loading Started ===");

        // Load local files
        logger.info("Attempting to load documents from local files...");
        List<Document> docs = new ArrayList<>(localFileService.loadLocalFiles());
        logger.info("Loaded " + docs.size() + " documents from local files.");

        // Load Confluence docs if enabled
        if (config.useConfluence) {
            logger.info("Confluence integration enabled. Fetching documents from Confluence...");
            List<Document> confluenceDocs = confluenceService.fetchPages();
            logger.info("Retrieved " + confluenceDocs.size() + " documents from Confluence.");
            docs.addAll(confluenceDocs);
        } else {
            logger.info("Confluence integration disabled. Skipping Confluence fetch.");
        }

        logger.info("=== Document Loading Completed. Total documents: " + docs.size() + " ===");
        return docs;
    }
}
