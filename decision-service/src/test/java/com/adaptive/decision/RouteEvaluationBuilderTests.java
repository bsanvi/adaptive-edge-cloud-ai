
package com.adaptive.decision;

import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.RouteEvaluationBuilder;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteEvaluationBuilderTests {

    private DecisionRequest request(String privacy) {
        return new DecisionRequest(
            new TaskProfile(
                "task-001",
                "SENSOR_ANOMALY",
                1024L,
                "LOW",
                1000.0,
                privacy,
                "BALANCED"
            ),
            new DeviceMetrics(
                Instant.now(),
                30.0,
                40.0,
                80.0,
                true
            ),
            new NetworkMetrics(
                true,
                20.0,
                50.0
            ),
            null
        );
    }

    private RouteEstimate estimate(
            boolean available,
            Double latency) {

        return new RouteEstimate(
            available,
            available ? null : "UNAVAILABLE",
            latency,
            100.0,
            40.0,
            50.0,
            10.0,
            1.0,
            1024L,
            128L,
            0.01,
            0.3,
            "SIMULATED"
        );
    }

    private EstimatorResponse response() {
        return new EstimatorResponse(
            "task-001",
            "test-v1",
            "SIMULATED",
            Map.of(
                "EDGE", estimate(true, 500.0),
                "CLOUD", estimate(true, 200.0),
                "HYBRID", estimate(true, 350.0)
            )
        );
    }

    private RouteCostInputs costs() {
        return new RouteCostInputs(
            Map.of(
                "EDGE", 0.1,
                "CLOUD", 0.2,
                "HYBRID", 0.15
            )
        );
    }

    @Test
    void buildsAllThreeRoutes() {

        var result = RouteEvaluationBuilder.build(
            request("PUBLIC"),
            response(),
            costs()
        );

        assertEquals(3, result.size());
        assertEquals("EDGE", result.get(0).route());
        assertEquals("CLOUD", result.get(1).route());
        assertEquals("HYBRID", result.get(2).route());
    }

    @Test
    void rejectsCloudForPrivateTask() {

        var result = RouteEvaluationBuilder.build(
            request("PRIVATE"),
            response(),
            costs()
        );

        assertTrue(result.get(0).feasible());
        assertFalse(result.get(1).feasible());
        assertFalse(result.get(2).feasible());

        assertTrue(
            result.get(1).rejectionReasons()
                .contains("PRIVACY_RESTRICTION")
        );
    }

    @Test
    void rejectsUnavailableEstimate() {

        var estimatorResponse = new EstimatorResponse(
            "task-001",
            "test-v1",
            "SIMULATED",
            Map.of(
                "EDGE", estimate(true, 500.0),
                "CLOUD", estimate(false, null),
                "HYBRID", estimate(true, 350.0)
            )
        );

        var result = RouteEvaluationBuilder.build(
            request("PUBLIC"),
            estimatorResponse,
            costs()
        );

        assertFalse(result.get(1).feasible());

        assertTrue(
            result.get(1).rejectionReasons()
                .contains("ESTIMATE_UNAVAILABLE")
        );
    }

    @Test
    void rejectsMissingEstimate() {

        var estimatorResponse = new EstimatorResponse(
            "task-001",
            "test-v1",
            "SIMULATED",
            Map.of(
                "EDGE", estimate(true, 500.0)
            )
        );

        var result = RouteEvaluationBuilder.build(
            request("PUBLIC"),
            estimatorResponse,
            costs()
        );

        assertTrue(result.get(0).feasible());
        assertFalse(result.get(1).feasible());
        assertFalse(result.get(2).feasible());
    }

    @Test
    void rejectsMismatchedTaskId() {

        var invalid = new EstimatorResponse(
            "wrong-task",
            "test-v1",
            "SIMULATED",
            response().estimates()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> RouteEvaluationBuilder.build(
                request("PUBLIC"),
                invalid,
                costs()
            )
        );
    }

    @Test
    void rejectsMissingMonetaryCost() {

        var missing = new RouteCostInputs(
            Map.of("EDGE", 0.1)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> RouteEvaluationBuilder.build(
                request("PUBLIC"),
                response(),
                missing
            )
        );
    }
}
