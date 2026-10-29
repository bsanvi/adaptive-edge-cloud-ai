package com.adaptive.decision.dto;

public record TaskProfile(
    String taskId,
    String taskType,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) long inputDataSizeBytes,
    String complexity,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) double deadlineMs,
    String privacyLevel,
    String priorityProfile
) {
}