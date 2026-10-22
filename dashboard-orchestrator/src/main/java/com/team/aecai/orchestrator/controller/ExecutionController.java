
package com.team.aecai.orchestrator.controller;

import com.team.aecai.orchestrator.dto.TaskRequest;
import com.team.aecai.orchestrator.dto.OrchestrationResult;
import com.team.aecai.orchestrator.dto.ExecutionHistoryEntry;

import com.team.aecai.orchestrator.orchestration.AdaptiveOrchestrator;
import com.team.aecai.orchestrator.repository.ExecutionHistoryRepository;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class ExecutionController {

    private final AdaptiveOrchestrator orchestrator;
    private final ExecutionHistoryRepository history;

    public ExecutionController(
            AdaptiveOrchestrator orchestrator,
            ExecutionHistoryRepository history) {

        this.orchestrator = orchestrator;
        this.history = history;
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

        long start = System.nanoTime();

        try {
            OrchestrationResult result =
                    orchestrator.execute(request, currentRoute);

            history.save(result);

            return result;

        } catch (RuntimeException ex) {

            double elapsedMs =
                    (System.nanoTime() - start)
                    / 1_000_000.0;

            // Record a failed execution attempt.
            OrchestrationResult failed =
                    new OrchestrationResult(
                            request.taskId(),
                            "FAILED",
                            null,
                            null,
                            false,
                            null,
                            null,
                            elapsedMs,
                            "Execution failed: "
                                    + ex.getClass().getSimpleName()
                    );

            try {
                history.save(failed);
            } catch (RuntimeException loggingError) {
                // Preserve the original execution error.
                ex.addSuppressed(loggingError);
            }

            throw ex;
        }
    }

    @GetMapping("/history")
    public List<ExecutionHistoryEntry> getHistory(
            @RequestParam(defaultValue = "20") int limit) {

        return history.findRecent(limit);
    }
}
