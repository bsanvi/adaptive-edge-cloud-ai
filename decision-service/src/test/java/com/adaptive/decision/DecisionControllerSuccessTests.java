
package com.adaptive.decision;

import com.adaptive.decision.controller.DecisionController;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DecisionControllerSuccessTests {

    @Test
    void returnsCompleteDecisionResponse() {

        DecisionInputsProvider provider =
            mock(DecisionInputsProvider.class);

        DecisionPipeline pipeline =
            mock(DecisionPipeline.class);

        DecisionRequest request = new DecisionRequest(
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

        EstimatorInputs estimatorInputs =
            new EstimatorInputs(
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

        DecisionSupportingInputs inputs =
            new DecisionSupportingInputs(
                estimatorInputs,
                new RouteCostInputs(
                    Map.of("EDGE", 0.1)
                ),
                Map.of("EDGE", 0.01),
                Map.of("EDGE", 0.0)
            );

        when(provider.resolve(any(DecisionRequest.class)))
            .thenReturn(inputs);

        RouteEstimate edgeEstimate = new RouteEstimate(
            true,
            null,
            500.0,
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

        EstimatorResponse estimates =
            new EstimatorResponse(
                "task-001",
                "test-v1",
                "SIMULATED",
                Map.of("EDGE", edgeEstimate)
            );

        RoutingResult routing = new RoutingResult(
            "ROUTE_SELECTED",
            "EDGE",
            List.of(
                new RoutingResult.RouteResult(
                    "EDGE",
                    true,
                    0.85,
                    List.of()
                )
            )
        );

        when(pipeline.evaluateDetailed(
            any(DecisionRequest.class),
            any(EstimatorInputs.class),
            any(RouteCostInputs.class)
        )).thenReturn(
            new DecisionPipelineResult(
                routing,
                estimates
            )
        );

        DecisionController controller =
            new DecisionController(provider, pipeline);

        var httpResponse = controller.evaluate(request);

        assertEquals(200, httpResponse.getStatusCode().value());

        assertInstanceOf(
            DecisionResponse.class,
            httpResponse.getBody()
        );

        DecisionResponse response =
            (DecisionResponse) httpResponse.getBody();

        assertNotNull(response);
        assertEquals("task-001", response.taskId());
        assertEquals("ROUTE_SELECTED", response.status());
        assertEquals("EDGE", response.selectedRoute());
        assertEquals(1, response.evaluations().size());
        assertTrue(response.decisionTimeMs() >= 0);
        assertFalse(response.reasons().isEmpty());
    }
}
