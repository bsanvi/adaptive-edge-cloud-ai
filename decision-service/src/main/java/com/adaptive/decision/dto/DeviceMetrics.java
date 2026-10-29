package com.adaptive.decision.dto;

import java.time.Instant;

public record DeviceMetrics(
    Instant timestamp,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) double cpuPercent,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) double ramPercent,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) double batteryPercent,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) boolean batteryAvailable
) {
}