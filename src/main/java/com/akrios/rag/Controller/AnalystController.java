package com.akrios.rag.Controller;

import com.akrios.rag.Service.RequirementsAnalystService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analyst")
public class AnalystController {

    private final RequirementsAnalystService analystService;

    public AnalystController(RequirementsAnalystService analystService) {
        this.analystService = analystService;
    }

    @PostMapping("/analyst/interact")
    public Map<String, String> analystInteract(
            @RequestParam String userId,
            @RequestBody Map<String, String> body) {

        String message = body.get("message");
        String response = analystService.interact(userId, message);

        return Map.of("response", response);
    }

}

