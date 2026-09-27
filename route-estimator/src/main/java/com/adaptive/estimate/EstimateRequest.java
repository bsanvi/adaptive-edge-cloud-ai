package com.adaptive.estimate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record EstimateRequest(@NotBlank @Size(max=100) String requestId,
    @NotNull @Valid Task task, @Valid Device device, @NotNull @Valid Network network,
    @NotNull Boolean edgeCapable, @NotNull Boolean cloudCapable,
    @PositiveOrZero Double remainingDeadlineMs,
    @NotNull @PositiveOrZero Double edgeCarbonIntensityGPerKwh,
    @NotNull @PositiveOrZero Double cloudCarbonIntensityGPerKwh) {
    public enum Complexity { LOW, MEDIUM, HIGH }
    public enum Source { MEASURED, SIMULATED, ESTIMATED, CALIBRATED }
    public record Task(@NotBlank @Size(max=100) String taskId, @Pattern(regexp="SENSOR_ANOMALY") @NotNull String taskType,
                       @NotNull Complexity complexity,
                       @Min(1) @Max(1048576) long dataSizeBytes,
                       @Min(1) @Max(1048576) long estimatedFeatureBytes,
                       @Min(1) @Max(1048576) long estimatedResultBytes) {}
    public record Device(@DecimalMin("0") @DecimalMax("100") Double cpuPercent,
                         @DecimalMin("0") @DecimalMax("100") Double ramPercent,
                         @DecimalMin("0") @DecimalMax("100") Double batteryPercent,
                         @NotNull Source source) {}
    public record Network(@NotNull Boolean connected, @Positive Double bandwidthMbps,
                          @PositiveOrZero Double networkLatencyMs, Instant sampledAt,
                          @NotNull Source source) {}
}
