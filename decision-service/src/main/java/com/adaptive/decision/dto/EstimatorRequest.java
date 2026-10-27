
package com.adaptive.decision.dto;

import java.time.Instant;

public record EstimatorRequest(
    String requestId,
    Task task,
    Device device,
    Network network,
    Boolean edgeCapable,
    Boolean cloudCapable,
    Double remainingDeadlineMs,
    Double edgeCarbonIntensityGPerKwh,
    Double cloudCarbonIntensityGPerKwh
) {

    public record Task(
        String taskId,
        String taskType,
        String complexity,
        long dataSizeBytes,
        long estimatedFeatureBytes,
        long estimatedResultBytes
    ) {}

    public record Device(
        Double cpuPercent,
        Double ramPercent,
        Double batteryPercent,
        String source
    ) {}

    public record Network(
        Boolean connected,
        Double bandwidthMbps,
        Double networkLatencyMs,
        Instant sampledAt,
        String source
    ) {}
}
