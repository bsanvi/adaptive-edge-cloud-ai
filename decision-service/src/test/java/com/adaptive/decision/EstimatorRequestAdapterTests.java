
package com.adaptive.decision;

import com.adaptive.decision.adapter.EstimatorRequestAdapter;
import com.adaptive.decision.dto.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EstimatorRequestAdapterTests {

    private DecisionRequest validRequest() {
        return new DecisionRequest(
            new TaskProfile(
                "task-001",
                "SENSOR_ANOMALY",
                1024L,
                "LOW",
                1000.0,
                "PUBLIC",
                "BALANCED"
            ),
            new DeviceMetrics(
                Instant.now(),
                30.0, 40.0, 80.0, true
            ),
            new NetworkMetrics(
                true, 20.0, 50.0
            ),
            null
        );
    }

    private EstimatorInputs validInputs() {
        return new EstimatorInputs(
            256L,
            128L,
            true,
            true,
            500.0,
            400.0,
            Instant.now(),
            "SIMULATED",
            "SIMULATED"
        );
    }

    @Test
    void correctlyMapsTaskFields() {
        var result = EstimatorRequestAdapter.adapt(
            validRequest(), validInputs()
        );

        assertEquals("task-001", result.requestId());
        assertEquals("SENSOR_ANOMALY",
                     result.task().taskType());
        assertEquals("LOW", result.task().complexity());
        assertEquals(1024L, result.task().dataSizeBytes());
        assertEquals(256L,
                     result.task().estimatedFeatureBytes());
    }

    @Test
    void correctlyMapsDeviceAndNetwork() {
        var result = EstimatorRequestAdapter.adapt(
            validRequest(), validInputs()
        );

        assertEquals(30.0, result.device().cpuPercent());
        assertEquals(20.0,
                     result.network().networkLatencyMs());
        assertEquals(50.0,
                     result.network().bandwidthMbps());
    }

    @Test
    void rejectsMissingInputs() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EstimatorRequestAdapter.adapt(
                validRequest(), null
            )
        );
    }

    @Test
    void rejectsUnsupportedTaskType() {
        var original = validRequest();

        var invalidTask = new TaskProfile(
            "task-001", "UNSUPPORTED",
            1024L, "LOW", 1000.0,
            "PUBLIC", "BALANCED"
        );

        var invalid = new DecisionRequest(
            invalidTask,
            original.device(),
            original.network(),
            null
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> EstimatorRequestAdapter.adapt(
                invalid, validInputs()
            )
        );
    }

    @Test
    void rejectsMissingNetworkTimestamp() {
        var inputs = validInputs();

        var invalid = new EstimatorInputs(
            inputs.estimatedFeatureBytes(),
            inputs.estimatedResultBytes(),
            inputs.edgeCapable(),
            inputs.cloudCapable(),
            inputs.edgeCarbonIntensityGPerKwh(),
            inputs.cloudCarbonIntensityGPerKwh(),
            null,
            inputs.deviceSource(),
            inputs.networkSource()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> EstimatorRequestAdapter.adapt(
                validRequest(), invalid
            )
        );
    }
}
