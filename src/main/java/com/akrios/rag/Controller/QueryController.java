package com.akrios.rag.Controller;

import com.akrios.rag.Service.RagService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class QueryController {

    private final RagService ragService;

    public QueryController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public Map<String, String> query(
            @RequestParam String userId,
            @RequestParam(defaultValue = "true") boolean multiQuery,
            @RequestBody Map<String, String> body) {

        String question = body.get("question");
        String answer = ragService.ask(userId, question, multiQuery);

        return Map.of("answer", answer);
    }
}
