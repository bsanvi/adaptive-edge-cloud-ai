
package com.adaptive.decision.adapter;

import com.adaptive.decision.dto.DecisionRequest;
import com.adaptive.decision.dto.EstimatorInputs;
import com.adaptive.decision.dto.EstimatorRequest;

import java.time.Instant;
import java.util.Set;

public class EstimatorRequestAdapter {

    private static final Set<String> COMPLEXITIES =
        Set.of("LOW", "MEDIUM", "HIGH");

    private static final Set<String> SOURCES =
        Set.of("MEASURED", "SIMULATED",
               "ESTIMATED", "CALIBRATED");

    public static EstimatorRequest adapt(
            DecisionRequest request,
            EstimatorInputs inputs) {

        if (request == null || inputs == null ||
                request.task() == null ||
                request.device() == null ||
                request.network() == null) {
            throw new IllegalArgumentException(
                "Decision request and estimator inputs are required"
            );
        }

        var task = request.task();
        var device = request.device();
        var network = request.network();

        if (task.taskId() == null ||
                task.taskId().isBlank() ||
                !"SENSOR_ANOMALY".equals(task.taskType())) {
            throw new IllegalArgumentException(
                "Unsupported or invalid task"
            );
        }

        if (!COMPLEXITIES.contains(task.complexity())) {
            throw new IllegalArgumentException(
                "Complexity must be LOW, MEDIUM or HIGH"
            );
        }

        if (task.inputDataSizeBytes() < 1 ||
                task.inputDataSizeBytes() > 1_048_576 ||
                inputs.estimatedFeatureBytes() < 1 ||
                inputs.estimatedFeatureBytes() > 1_048_576 ||
                inputs.estimatedResultBytes() < 1 ||
                inputs.estimatedResultBytes() > 1_048_576) {
            throw new IllegalArgumentException(
                "Task sizes must be between 1 and 1048576 bytes"
            );
        }

        if (!Double.isFinite(task.deadlineMs()) ||
                task.deadlineMs() <= 0) {
            throw new IllegalArgumentException(
                "Task deadline must be positive and finite"
            );
        }

        if (inputs.networkSampledAt() == null ||
                inputs.networkSampledAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                "A valid network measurement timestamp is required"
            );
        }

        if (!SOURCES.contains(inputs.deviceSource()) ||
                !SOURCES.contains(inputs.networkSource())) {
            throw new IllegalArgumentException(
                "Valid measurement sources are required"
            );
        }

        if (!validPercent(device.cpuPercent()) ||
                !validPercent(device.ramPercent()) ||
                (device.batteryAvailable() && !validPercent(device.batteryPercent()))) {
            throw new IllegalArgumentException(
                "Device measurements must be valid percentages"
            );
        }

        if (!Double.isFinite(network.latencyMs()) ||
                network.latencyMs() < 0 ||
                !Double.isFinite(network.bandwidthMbps()) ||
                network.bandwidthMbps() < 0 || (network.connected() && network.bandwidthMbps() == 0)) {
            throw new IllegalArgumentException(
                "Network measurements are invalid"
            );
        }

        if (!finiteNonnegative(
                inputs.edgeCarbonIntensityGPerKwh()) ||
                !finiteNonnegative(
                inputs.cloudCarbonIntensityGPerKwh())) {
            throw new IllegalArgumentException(
                "Carbon intensity values are invalid"
            );
        }

        return new EstimatorRequest(
            task.taskId(),
            new EstimatorRequest.Task(
                task.taskId(),
                task.taskType(),
                task.complexity(),
                task.inputDataSizeBytes(),
                inputs.estimatedFeatureBytes(),
                inputs.estimatedResultBytes()
            ),
            new EstimatorRequest.Device(
                device.cpuPercent(),
                device.ramPercent(),
                device.batteryAvailable()
                    ? device.batteryPercent()
                    : null,
                inputs.deviceSource()
            ),
            new EstimatorRequest.Network(
                network.connected(),
                network.connected() ? network.bandwidthMbps() : null,
                network.connected() ? network.latencyMs() : null,
                inputs.networkSampledAt(),
                inputs.networkSource()
            ),
            inputs.edgeCapable(),
            inputs.cloudCapable(),
            task.deadlineMs(),
            inputs.edgeCarbonIntensityGPerKwh(),
            inputs.cloudCarbonIntensityGPerKwh()
        );
    }

    private static boolean validPercent(double value) {
        return Double.isFinite(value)
            && value >= 0 && value <= 100;
    }

    private static boolean finiteNonnegative(double value) {
        return Double.isFinite(value) && value >= 0;
    }
}
