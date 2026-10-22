
package com.team.aecai.orchestrator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/v1/orchestrator/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "component", "orchestrator"
        );
    }
}
