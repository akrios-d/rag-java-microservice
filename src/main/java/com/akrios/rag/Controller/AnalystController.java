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

    @PostMapping("/interact")
    public Map<String, Object> interact(
            @RequestParam String userId,
            @RequestBody Map<String, String> body) {
        String userMessage = body.get("message");
        return analystService.interact(userId, userMessage);
    }

    @GetMapping("/generate")
    public String generate(@RequestParam String userId) {
        return analystService.finalizeRequirements(userId);
    }
}

