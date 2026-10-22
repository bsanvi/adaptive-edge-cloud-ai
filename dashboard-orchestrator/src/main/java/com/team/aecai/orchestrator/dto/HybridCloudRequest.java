
package com.team.aecai.orchestrator.dto;

import java.util.List;

public record HybridCloudRequest(
    String taskId,
    List<Double> processedFeatures
) {
}
