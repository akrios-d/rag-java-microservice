package com.akrios.rag.Service.DocumentsLoader;

import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LocalFileService {

    public List<Document> loadLocalFiles() {
        List<Document> docs = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            // Load all files under resources/local_files
            Resource[] resources = resolver.getResources("classpath:local_files/*");

            for (Resource res : resources) {
                if (res.isFile()) {
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                    }
                    docs.add(Document.builder()
                            .text(content.toString())
                            .metadata(Map.of(
                                    "source", "local_file",
                                    "path", res.getFilename() != null ? res.getFilename() : "unknown"
                            ))
                            .build());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return docs;
    }
}