package com.adaptive.decision.dto;

public record NetworkMetrics(
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) boolean connected,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) double latencyMs,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) double bandwidthMbps
) {
}