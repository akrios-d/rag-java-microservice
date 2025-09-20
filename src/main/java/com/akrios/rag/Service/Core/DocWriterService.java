package com.akrios.rag.Service.Core;

import com.akrios.rag.Prompts.PromptTemplates;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class DocWriterService {

    private static final Logger logger = Logger.getLogger(DocWriterService.class.getName());

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
        logger.info("Generating documentation from collected requirements...");
        String prompt = PromptTemplates.SYSTEM_DOC_WRITER
                + "\n\nRequirements:\n" + collectedRequirements;

        String document = chatModel.call(prompt);
        logger.info("Documentation generation complete. Length: " + (document != null ? document.length() : 0));
        return document;
    }
}

