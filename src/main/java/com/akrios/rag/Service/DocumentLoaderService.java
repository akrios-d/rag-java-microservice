package com.akrios.rag.Service;

import com.akrios.rag.Config.DocumentLoaderConfig;
import com.akrios.rag.Service.DocumentsLoader.ConfluenceService;
import com.akrios.rag.Service.DocumentsLoader.LocalFileService;
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
        logger.info("Loading documents...");

        logger.info("Loading documents from local files...");
        List<Document> localDocs = localFileService.loadLocalFiles();
        List<Document> documents = new ArrayList<>(localDocs);
        logger.info("Loaded " + localDocs.size() + " documents from local files.");

        if (config.useConfluence) {
            logger.info("Fetching documents from Confluence...");
            List<Document> confluenceDocs = confluenceService.fetchPages();
            documents.addAll(confluenceDocs);
            logger.info("Loaded " + confluenceDocs.size() + " documents from Confluence.");
        }

        logger.info("Total " + documents.size() + " documents loaded.");
        return documents;
    }
}