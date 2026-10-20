
package com.team.aecai.orchestrator.dto;

public record InferenceResponse(
    String taskId,
    String prediction,
    double confidence,
    double inferenceTimeMs
) {
}
