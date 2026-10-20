
package com.team.aecai.orchestrator.controller;

import com.team.aecai.orchestrator.dto.*;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private static final Set<String> ROUTES =
            Set.of("EDGE", "CLOUD", "HYBRID");

    @PostMapping(
        value = "/execute",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OrchestrationResult> execute(
            @RequestBody TaskRequest task,
            @RequestParam(defaultValue = "EDGE") String route) {

        String chosenRoute = route.toUpperCase();

        if (!ROUTES.contains(chosenRoute)) {
            return ResponseEntity.badRequest().build();
        }

        if (task.taskId() == null ||
                task.taskId().isBlank() ||
                task.features() == null ||
                task.features().size() != 4) {

            return ResponseEntity.badRequest().build();
        }

        // Synthetic prediction for demonstration only.
        // This does NOT run an AI model.
        InferenceResponse prediction =
                new InferenceResponse(
                        task.taskId(),
                        "SETOSA",
                        0.94,
                        12.0
                );

        RouteEvaluation evaluation =
                new RouteEvaluation(
                        chosenRoute,
                        true,
                        25.0,
                        2.0,
                        0.001,
                        0.0,
                        0.0,
                        0.25,
                        List.of()
                );

        DecisionResponse decision =
                new DecisionResponse(
                        task.taskId(),
                        "ROUTE_SELECTED",
                        chosenRoute,
                        2.0,
                        List.of(evaluation),
                        List.of("SIMULATED DEMO DECISION")
                );

        OrchestrationResult result =
                new OrchestrationResult(
                        task.taskId(),
                        "SIMULATED",
                        chosenRoute,
                        chosenRoute,
                        false,
                        prediction,
                        decision,
                        25.0,
                        "DEMO ONLY: synthetic prediction " +
                        "and timings; no real inference performed"
                );

        return ResponseEntity.ok(result);
    }
}
