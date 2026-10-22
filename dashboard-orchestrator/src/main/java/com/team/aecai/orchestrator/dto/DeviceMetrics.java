
package com.team.aecai.orchestrator.dto;

import java.time.Instant;

public record DeviceMetrics(
    Instant timestamp,
    double cpuPercent,
    double ramPercent,
    double batteryPercent,
    boolean batteryAvailable
) {
}
