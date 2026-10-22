
package com.team.aecai.orchestrator.controller;

import com.team.aecai.orchestrator.dto.EvaluationSummary;
import com.team.aecai.orchestrator.evaluation.EvaluationService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(
            EvaluationService evaluationService) {

        this.evaluationService = evaluationService;
    }

    @GetMapping("/summary")
    public EvaluationSummary getSummary() {
        return evaluationService.getSummary();
    }
}
