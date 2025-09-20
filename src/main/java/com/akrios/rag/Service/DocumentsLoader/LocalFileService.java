package com.akrios.rag.Service.DocumentsLoader;

import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
            // Adjust the folder path inside resources
            Resource folder = new ClassPathResource("local_files");
            Resource[] resources = folder.getFile().listFiles() != null ?
                    (Resource[]) folder.getFile().listFiles() : new Resource[0];

            assert resources != null;
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
                            .metadata(Map.of("source", "local_file", "path", Objects.requireNonNull(res.getFilename())))
                            .build());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return docs;
    }
}