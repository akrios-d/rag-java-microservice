package com.akrios.rag.Service.Core;


import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EtlPipelineService {

    private final DocumentReader myReader;
    private final DocumentTransformer splitter;
    private final DocumentWriter writer;


    public EtlPipelineService(VectorStore vectorStore, DocumentLoaderService loaderService, VectorStoreService vectorStoreService) {
        // Reader: load from your local files
        this.myReader = loaderService::loadDocuments;  // loaderService returns List<Document>

        // Transformer: split into chunks
        this.splitter = vectorStoreService::chunkDocuments;

        // Writer: accept and write into vector store
        this.writer = vectorStore;
    }

    public void runEtl() {
        List<Document> raw = myReader.get();
        List<Document> transformed = splitter.apply(raw);
        writer.accept(transformed);
    }
}