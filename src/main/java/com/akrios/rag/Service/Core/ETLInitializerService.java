package com.akrios.rag.Service.Core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;

@Service
@Slf4j
public class ETLInitializerService {

    private static final Logger logger = Logger.getLogger(ETLInitializerService.class.getName());
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path EMBEDDINGS_FILE = DATA_DIR.resolve("embeddings.json");
    private static final Path HASH_FILE = DATA_DIR.resolve("embeddings.hash");

    private final DocumentLoaderService loaderService;
    private final VectorStoreService vectorStoreService;

    public ETLInitializerService(DocumentLoaderService loaderService, VectorStoreService vectorStoreService) {
        this.loaderService = loaderService;
        this.vectorStoreService = vectorStoreService;
    }

    public void initialize() {
        try {
            logger.info("🚀 Starting ETL initialization process...");

            List<Document> rawDocs = loaderService.loadDocuments();
            String newHash = computeDocumentsHash(rawDocs);

            if (Files.exists(HASH_FILE)) {
                String existingHash = Files.readString(HASH_FILE, StandardCharsets.UTF_8);
                if (existingHash.equals(newHash) && Files.exists(EMBEDDINGS_FILE)) {
                    logger.info("✅ No document changes detected. Loading embeddings from disk...");
                    List<Document> cachedDocs = loadEmbeddingsFromDisk();
                    vectorStoreService.initializeFromCached(cachedDocs);
                    logger.info("📚 Loaded " + cachedDocs.size() + " chunks from cache.");
                    return;
                }
            }

            logger.info("📄 Changes detected or no cache found. Rebuilding embeddings...");
            List<Document> chunkedDocs = vectorStoreService.chunkDocuments(rawDocs);
            vectorStoreService.initializeFromScratch(chunkedDocs);

            saveEmbeddingsToDisk(chunkedDocs);
            Files.writeString(HASH_FILE, newHash, StandardCharsets.UTF_8);

            logger.info("✅ ETL process completed. Embeddings persisted successfully.");
        } catch (Exception e) {
            logger.severe("❌ ETL initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String computeDocumentsHash(List<Document> docs) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (Document doc : docs) {
            digest.update(doc.getText().getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(digest.digest());
    }

    private void saveEmbeddingsToDisk(List<Document> docs) throws IOException {
        Files.createDirectories(DATA_DIR);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(EMBEDDINGS_FILE.toFile(), docs);
        logger.info("💾 Embeddings saved to " + EMBEDDINGS_FILE.toAbsolutePath());
    }

    private List<Document> loadEmbeddingsFromDisk() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(EMBEDDINGS_FILE.toFile(), new TypeReference<>() {});
    }
}
