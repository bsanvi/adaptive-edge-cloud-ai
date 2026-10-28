package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;
import com.adaptive.decision.scoring.ScoringConfiguration;
import java.util.Set;

/** Shared validation runs before provider lookup or any estimator HTTP call. */
public final class DecisionRequestValidator {
    private DecisionRequestValidator() {}
    public static void validate(DecisionRequest request, ScoringConfiguration config) {
        if (request == null || request.task() == null || request.device() == null || request.network() == null)
            throw new IllegalArgumentException("Missing required task, device or network");
        var task = request.task();
        if (task.taskId() == null || task.taskId().isBlank() || task.taskId().length() > 100)
            throw new IllegalArgumentException("taskId must contain 1 to 100 characters");
        if (!"SENSOR_ANOMALY".equals(task.taskType())) throw new IllegalArgumentException("Unsupported task type");
        if (task.complexity() == null || !Set.of("LOW", "MEDIUM", "HIGH").contains(task.complexity()))
            throw new IllegalArgumentException("Complexity must be LOW, MEDIUM or HIGH");
        if (task.privacyLevel() == null || !Set.of("PUBLIC", "PRIVATE", "NORMAL", "CRITICAL").contains(task.privacyLevel().trim().toUpperCase(java.util.Locale.ROOT)))
            throw new IllegalArgumentException("Unsupported privacy level");
        if (task.inputDataSizeBytes() < 1 || task.inputDataSizeBytes() > 1_048_576)
            throw new IllegalArgumentException("Input size must be between 1 and 1048576 bytes");
        if (!Double.isFinite(task.deadlineMs()) || task.deadlineMs() <= 0)
            throw new IllegalArgumentException("Deadline must be positive and finite");
        config.weights(task.priorityProfile());
        var device = request.device();
        if (device.timestamp() == null) throw new IllegalArgumentException("Device sample timestamp is required");
        if (!percent(device.cpuPercent()) || !percent(device.ramPercent())
                || (device.batteryAvailable() && !percent(device.batteryPercent())))
            throw new IllegalArgumentException("Device metrics must be percentages between 0 and 100");
        var network = request.network();
        if (!Double.isFinite(network.latencyMs()) || network.latencyMs() < 0
                || !Double.isFinite(network.bandwidthMbps()) || network.bandwidthMbps() < 0
                || (network.connected() && network.bandwidthMbps() == 0))
            throw new IllegalArgumentException("Network metrics must be finite and valid for connectivity");
        if (request.currentRoute() != null && !request.currentRoute().isBlank()
                && !Set.of("EDGE", "CLOUD", "HYBRID").contains(request.currentRoute()))
            throw new IllegalArgumentException("Unknown currentRoute");
    }
    private static boolean percent(double value) { return Double.isFinite(value) && value >= 0 && value <= 100; }
}
