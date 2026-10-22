
package com.team.aecai.orchestrator.dto;

import java.util.List;

public record InferenceRequest(
    String taskId,
    List<Double> features
) {
}
