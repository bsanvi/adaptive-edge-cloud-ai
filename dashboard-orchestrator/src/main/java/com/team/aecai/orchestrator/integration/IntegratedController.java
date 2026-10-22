package com.team.aecai.orchestrator.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Enable only for local/demo integration; do not expose to the public internet. */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/integrated")
public class IntegratedController {
    private final IntegratedOrchestrator orchestrator;
    private final IntegratedHistory history;
    public IntegratedController(IntegratedOrchestrator orchestrator, IntegratedHistory history) {
        this.orchestrator = orchestrator;
        this.history = history;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "component", "integrated-orchestrator");
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody IntegratedOrchestrator.RunTask task) {
        try {
            return ResponseEntity.ok(orchestrator.run(task));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", error.getMessage()));
        }
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history(@RequestParam(defaultValue = "20") int limit) {
        return history.recent(limit);
    }
}
