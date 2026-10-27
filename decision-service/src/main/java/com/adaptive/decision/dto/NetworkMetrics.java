package com.adaptive.decision.dto;

public record NetworkMetrics(
    boolean connected,
    double latencyMs,
    double bandwidthMbps
) {
}