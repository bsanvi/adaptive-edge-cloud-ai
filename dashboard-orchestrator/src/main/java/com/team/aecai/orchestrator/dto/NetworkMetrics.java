
package com.team.aecai.orchestrator.dto;

public record NetworkMetrics(
    boolean connected,
    double latencyMs,
    double bandwidthMbps
) {
}
