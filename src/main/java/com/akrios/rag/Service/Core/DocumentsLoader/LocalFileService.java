package com.akrios.rag.Service.Core.DocumentsLoader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Slf4j
public class LocalFileService {

    private static final String LOCAL_FILES_PATH = "classpath:local_files/*";

    public List<Document> loadLocalFiles() {
        log.info("=== Local File Loader Started ===");
        List<Document> docs = new ArrayList<>();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(LOCAL_FILES_PATH);

            log.info("Found {} resources in " + LOCAL_FILES_PATH, resources.length);

            for (Resource res : resources) {
                if (!res.exists() || !res.isFile()) {
                    log.warn("Skipping resource (not a file or does not exist): {}", res.getFilename());
                    continue;
                }

                String filename = res.getFilename() != null ? res.getFilename() : "unknown";
                String fileType = getFileExtension(filename);

                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                } catch (Exception ex) {
                    log.error("Failed to read file: {}", filename, ex);
                    continue; // skip this file but continue with others
                }

                if (content.isEmpty()) {
                    log.warn("Skipping empty file: {}", filename);
                    continue;
                }

                Document doc = Document.builder()
                        .text(content.toString())
                        .metadata(Map.of(
                                "source", "local_file",
                                "filename", filename,
                                "file_type", fileType,
                                "size", String.valueOf(content.length())
                        ))
                        .build();

                docs.add(doc);
                log.info("Loaded file: {} | type={} | size={} chars", filename, fileType, content.length());
            }
        } catch (Exception e) {
            log.error("Error resolving resources from path: " + LOCAL_FILES_PATH, e);
        }

        log.info("=== Local File Loader Completed. Total documents: {} ===", docs.size());
        return docs;
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0 && idx < filename.length() - 1) ? filename.substring(idx + 1).toLowerCase() : "unknown";
    }
}
