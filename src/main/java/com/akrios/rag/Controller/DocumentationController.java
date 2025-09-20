package com.akrios.rag.Controller;


import com.akrios.rag.Service.DocWriterService;
import com.akrios.rag.Service.RequirementsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/docs")
public class DocumentationController {

    private final RequirementsService requirementsService;
    private final DocWriterService docWriterService;

    public DocumentationController(RequirementsService requirementsService, DocWriterService docWriterService) {
        this.requirementsService = requirementsService;
        this.docWriterService = docWriterService;
    }

    @PostMapping("/requirements")
    public Map<String, String> askRequirements(@RequestBody Map<String, String> body) {
        String userInput = body.get("input");
        String response = requirementsService.askRequirements(userInput);
        return Map.of("response", response);
    }

    @PostMapping("/generate")
    public Map<String, String> generateDoc(@RequestBody Map<String, String> body) {
        String collectedRequirements = body.get("requirements");
        String doc = docWriterService.generateDocumentation(collectedRequirements);
        return Map.of("document", doc);
    }
}