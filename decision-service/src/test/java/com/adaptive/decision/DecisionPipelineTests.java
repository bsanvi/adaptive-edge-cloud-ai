
package com.adaptive.decision;

import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DecisionPipelineTests {

    private DecisionRequest request(
            String privacy,
            String previousRoute) {

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
            previousRoute
        );
    }

    private EstimatorInputs inputs() {
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

    private RouteCostInputs costs() {
        return new RouteCostInputs(
            Map.of(
                "EDGE", 0.1,
                "CLOUD", 0.1,
                "HYBRID", 0.1
            )
        );
    }

    private RouteEstimate estimate(double latency) {
        return new RouteEstimate(
            true,
            null,
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
                "EDGE", estimate(700.0),
                "CLOUD", estimate(200.0),
                "HYBRID", estimate(400.0)
            )
        );
    }

    @Test
    void selectsCloudForPublicTask() {

        EstimationGateway gateway =
            mock(EstimationGateway.class);

        when(gateway.estimate(
            any(DecisionRequest.class),
            any(EstimatorInputs.class)
        )).thenReturn(response());

        DecisionPipeline pipeline =
            new DecisionPipeline(gateway);

        RoutingResult result = pipeline.evaluate(
            request("PUBLIC", null),
            inputs(),
            costs()
        );

        assertEquals(
            "ROUTE_SELECTED",
            result.status()
        );

        assertEquals(
            "CLOUD",
            result.selectedRoute()
        );

        assertEquals(
            3,
            result.evaluations().size()
        );

        verify(gateway, times(1)).estimate(
            any(DecisionRequest.class),
            any(EstimatorInputs.class)
        );
    }

    @Test
    void selectsEdgeForPrivateTask() {

        EstimationGateway gateway =
            mock(EstimationGateway.class);

        when(gateway.estimate(
            any(DecisionRequest.class),
            any(EstimatorInputs.class)
        )).thenReturn(response());

        DecisionPipeline pipeline =
            new DecisionPipeline(gateway);

        RoutingResult result = pipeline.evaluate(
            request("PRIVATE", null),
            inputs(),
            costs()
        );

        assertEquals(
            "EDGE",
            result.selectedRoute()
        );
    }

    @Test
    void retainsEdgeForSmallScoreImprovement() {

        EstimationGateway gateway =
            mock(EstimationGateway.class);

        EstimatorResponse closeScores =
            new EstimatorResponse(
                "task-001",
                "test-v1",
                "SIMULATED",
                Map.of(
                    "EDGE", estimate(400.0),
                    "CLOUD", estimate(350.0),
                    "HYBRID", estimate(600.0)
                )
            );

        when(gateway.estimate(
            any(DecisionRequest.class),
            any(EstimatorInputs.class)
        )).thenReturn(closeScores);

        DecisionPipeline pipeline =
            new DecisionPipeline(gateway);

        RoutingResult result = pipeline.evaluate(
            request("PUBLIC", "EDGE"),
            inputs(),
            costs()
        );

        assertEquals(
            "EDGE",
            result.selectedRoute()
        );
    }
    
@Test
void preservesEstimatorResponseForApiAssembly() {

    EstimationGateway gateway =
        mock(EstimationGateway.class);

    EstimatorResponse expected = response();

    when(gateway.estimate(
        any(DecisionRequest.class),
        any(EstimatorInputs.class)
    )).thenReturn(expected);

    DecisionPipeline pipeline =
        new DecisionPipeline(gateway);

    DecisionPipelineResult result =
        pipeline.evaluateDetailed(
            request("PUBLIC", null),
            inputs(),
            costs()
        );

    assertNotNull(result.routing());
    assertSame(expected, result.estimates());

    assertEquals(
        "ROUTE_SELECTED",
        result.routing().status()
    );

    assertEquals(
        "CLOUD",
        result.routing().selectedRoute()
    );
}

}
