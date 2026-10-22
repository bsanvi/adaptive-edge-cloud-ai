
package com.team.aecai.orchestrator.dto;

public record OrchestrationResult(
    String taskId,
    String status,
    String selectedRoute,
    String actualRoute,
    boolean fallbackUsed,
    InferenceResponse prediction,
    DecisionResponse decision,
    double totalTimeMs,
    String message
) {
}
