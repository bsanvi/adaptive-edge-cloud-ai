
package com.team.aecai.orchestrator.dto;

import java.util.Map;

public record EvaluationSummary(
    long totalExecutions,
    long successfulExecutions,
    long failedExecutions,
    long noFeasibleRouteExecutions,
    long fallbackCount,
    double successRatePercent,
    double averageExecutionTimeMs,
    Map<String, Long> routeCounts,
    Map<String, Double> averageTimeByRouteMs
) {
}
