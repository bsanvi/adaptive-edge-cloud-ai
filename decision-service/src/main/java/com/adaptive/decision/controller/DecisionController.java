
package com.adaptive.decision.controller;

import com.adaptive.decision.dto.DecisionRequest;
import com.adaptive.decision.dto.DecisionResponse;

import com.adaptive.decision.service.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/decision")
public class DecisionController {

    private final DecisionInputsProvider inputsProvider;
    private final DecisionPipeline decisionPipeline;

    public DecisionController(
            DecisionInputsProvider inputsProvider,
            DecisionPipeline decisionPipeline) {

        this.inputsProvider = inputsProvider;
        this.decisionPipeline = decisionPipeline;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluate(
            @RequestBody DecisionRequest request) {

        long start = System.nanoTime();

        if (request == null
                || request.task() == null
                || request.device() == null
                || request.network() == null) {

            return ResponseEntity.badRequest().body(
                Map.of(
                    "error",
                    "Missing required task, device or network"
                )
            );
        }

        if (request.task().taskId() == null
                || request.task().taskId().isBlank()) {

            return ResponseEntity.badRequest().body(
                Map.of(
                    "error", "taskId is required"
                )
            );
        }

        DecisionSupportingInputs supportingInputs;

        try {
            supportingInputs = inputsProvider.resolve(request);

        } catch (IllegalStateException ex) {

            return ResponseEntity.status(
                HttpStatus.SERVICE_UNAVAILABLE
            ).body(
                Map.of(
                    "status", "SERVICE_UNAVAILABLE",
                    "message", ex.getMessage()
                )
            );
        }

        try {
            DecisionPipelineResult result =
                decisionPipeline.evaluateDetailed(
                    request,
                    supportingInputs.estimatorInputs(),
                    supportingInputs.routeCostInputs()
                );

            double decisionTimeMs =
                (System.nanoTime() - start) / 1_000_000.0;

            DecisionResponse response =
                DecisionResponseAssembler.assemble(
                    request.task().taskId(),
                    result.routing(),
                    result.estimates(),
                    supportingInputs.communicationCosts(),
                    supportingInputs.switchingCosts(),
                    decisionTimeMs
                );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.badRequest().body(
                Map.of("error", ex.getMessage())
            );

        } catch (IllegalStateException ex) {

            return ResponseEntity.status(
                HttpStatus.SERVICE_UNAVAILABLE
            ).body(
                Map.of(
                    "status", "SERVICE_UNAVAILABLE",
                    "message", ex.getMessage()
                )
            );
        }
    }
}
