
package com.team.aecai.orchestrator.dto;

import java.util.List;

public record HybridPreprocessResponse(
    String taskId,
    List<Double> processedFeatures,
    double processingTimeMs
) {
}
