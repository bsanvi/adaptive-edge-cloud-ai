
package com.team.aecai.orchestrator.dto;

public record ExecutionHistoryEntry(
    long id,
    String taskId,
    String status,
    String selectedRoute,
    String actualRoute,
    boolean fallbackUsed,
    String prediction,
    Double confidence,
    double totalTimeMs,
    String message,
    String createdAt
) {
}
