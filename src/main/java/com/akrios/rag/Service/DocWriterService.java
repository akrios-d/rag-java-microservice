package com.akrios.rag.Service;

import com.akrios.rag.Prompts.PromptTemplates;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

@Service
public class DocWriterService {

    private final OllamaChatModel chatModel;

    public DocWriterService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateDocumentation(String collectedRequirements) {
        String prompt = PromptTemplates.SYSTEM_DOC_WRITER + "\n\nRequirements:\n" + collectedRequirements;
        return chatModel.call(prompt);
    }
}
