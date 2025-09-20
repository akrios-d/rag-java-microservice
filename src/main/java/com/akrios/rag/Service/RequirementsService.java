package com.akrios.rag.Service;

import com.akrios.rag.Prompts.PromptTemplates;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

@Service
public class RequirementsService {

    private final OllamaChatModel chatModel;

    public RequirementsService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askRequirements(String userInput) {
        String prompt = PromptTemplates.SYSTEM_REQUIREMENTS_ANALYST + "\n\nUser: " + userInput;
        return chatModel.call(prompt);
    }
}