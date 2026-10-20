
package com.team.aecai.orchestrator.controller;

import com.team.aecai.orchestrator.dto.TaskRequest;
import com.team.aecai.orchestrator.dto.OrchestrationResult;
import com.team.aecai.orchestrator.orchestration.AdaptiveOrchestrator;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class ExecutionController {

    private final AdaptiveOrchestrator orchestrator;

    public ExecutionController(
            AdaptiveOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping(
        value = "/execute",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public OrchestrationResult execute(
            @RequestBody TaskRequest request,
            @RequestParam(defaultValue = "EDGE")
            String currentRoute) {

        return orchestrator.execute(
                request,
                currentRoute
        );
    }
}
