package com.akrios.rag.Service.DocumentsLoader;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConfluenceService {

    public List<Document> fetchPages() {
        // TODO: implement actual API fetch
        return List.of(
                Document.builder()
                        .text("Sample Confluence page content")
                        .metadata(Map.of("source", "confluence", "pageId", "123"))
                        .build()
        );
    }
}