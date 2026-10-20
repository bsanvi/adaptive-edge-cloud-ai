
package com.team.aecai.orchestrator.dto;

public record TaskProfile(
    String taskId,
    String taskType,
    long inputDataSizeBytes,
    String complexity,
    double deadlineMs,
    String privacyLevel,
    String priorityProfile
) {
}

