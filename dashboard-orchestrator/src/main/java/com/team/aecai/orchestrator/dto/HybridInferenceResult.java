
package com.team.aecai.orchestrator.dto;

public record HybridInferenceResult(
    String taskId,
    int classIndex,
    double confidence,
    double inferenceTimeMs
) {
}
