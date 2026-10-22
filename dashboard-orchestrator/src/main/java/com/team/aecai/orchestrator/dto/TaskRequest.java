
package com.team.aecai.orchestrator.dto;

import java.util.List;

public record TaskRequest(
    String taskId,
    List<Double> features,
    double deadlineMs,
    String privacyLevel,
    String priorityProfile,
    String requestedComplexity
) {
}
