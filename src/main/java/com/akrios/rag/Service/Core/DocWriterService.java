package com.akrios.rag.Service.Core;

import com.akrios.rag.Prompts.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
@Slf4j
public class DocWriterService {

    private final OllamaChatModel chatModel;

    public DocWriterService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Generate final documentation from collected requirements.
     *
     * @param collectedRequirements All requirements collected from the analyst agent
     * @return The full documentation text
     */
    public String generateDocumentation(String collectedRequirements) {
        log.info("Generating documentation from collected requirements...");
        String prompt = PromptTemplates.SYSTEM_DOC_WRITER
                + "\n\nRequirements:\n" + collectedRequirements;

        String document = chatModel.call(prompt);
        log.info("Documentation generation complete. Length: {}", document != null ? document.length() : 0);
        return document;
    }
}

