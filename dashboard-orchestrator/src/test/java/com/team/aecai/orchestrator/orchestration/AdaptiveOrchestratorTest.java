
package com.team.aecai.orchestrator.orchestration;

import org.mockito.InOrder;
import com.team.aecai.orchestrator.client.*;
import com.team.aecai.orchestrator.dto.*;
import com.team.aecai.orchestrator.network.NetworkMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdaptiveOrchestratorTest {

    private EdgeApiClient edge;
    private DecisionApiClient decisionClient;
    private CloudApiClient cloud;
    private NetworkMonitor networkMonitor;
    private AdaptiveOrchestrator orchestrator;

    private TaskRequest task;

    @BeforeEach
    void setUp() {

        edge = mock(EdgeApiClient.class);
        decisionClient = mock(DecisionApiClient.class);
        cloud = mock(CloudApiClient.class);
        networkMonitor = mock(NetworkMonitor.class);

        orchestrator = new AdaptiveOrchestrator(
                edge, decisionClient, cloud, networkMonitor
        );

        task = new TaskRequest(
                "TASK-001",
                List.of(5.1, 3.5, 1.4, 0.2),
                500,
                "NORMAL",
                "BALANCED",
                "MEDIUM"
        );

        when(edge.profileTask(task)).thenReturn(
                new TaskProfile(
                        "TASK-001",
                        "IRIS_CLASSIFICATION",
                        32,
                        "MEDIUM",
                        500,
                        "NORMAL",
                        "BALANCED"
                )
        );

        when(edge.getMetrics()).thenReturn(
                new DeviceMetrics(
                        Instant.parse("2026-10-20T11:00:00Z"),
                        40.0, 55.0, 80.0, true
                )
        );

        when(networkMonitor.measureNetwork()).thenReturn(
                new NetworkMetrics(true, 20.0, 80.0)
        );
    }

    private DecisionResponse decision(
            String route,
            boolean edgeFeasible) {

        RouteEvaluation edgeEvaluation =
                new RouteEvaluation(
                        "EDGE",
                        edgeFeasible,
                        100, 5, 0.001,
                        0, 0, 0.4,
                        List.of()
                );

        return new DecisionResponse(
                "TASK-001",
                "ROUTE_SELECTED",
                route,
                2.0,
                List.of(edgeEvaluation),
                List.of("Selected by Decision Engine")
        );
    }

    private InferenceResponse prediction() {
        return new InferenceResponse(
                "TASK-001",
                "SETOSA",
                0.94,
                12.0
        );
    }

    @Test
    void shouldExecuteOnEdge() {

        when(decisionClient.evaluate(any()))
                .thenReturn(decision("EDGE", true));

        when(edge.predict(any()))
                .thenReturn(prediction());

        OrchestrationResult result =
                orchestrator.execute(task, "EDGE");

        assertEquals("SUCCESS", result.status());
        assertEquals("EDGE", result.actualRoute());
        assertFalse(result.fallbackUsed());
        assertEquals("SETOSA",
                result.prediction().prediction());

        verify(edge).predict(any());
        verify(cloud, never()).predict(any());
    }

    @Test
    void shouldExecuteOnCloud() {

        when(decisionClient.evaluate(any()))
                .thenReturn(decision("CLOUD", true));

        when(cloud.predict(any()))
                .thenReturn(prediction());

        OrchestrationResult result =
                orchestrator.execute(task, "EDGE");

        assertEquals("SUCCESS", result.status());
        assertEquals("CLOUD", result.selectedRoute());
        assertEquals("CLOUD", result.actualRoute());
        assertFalse(result.fallbackUsed());

        verify(cloud).predict(any());
    }

    @Test
    void shouldFallbackWhenCloudFails() {

        when(decisionClient.evaluate(any()))
                .thenReturn(decision("CLOUD", true));

        when(cloud.predict(any()))
                .thenThrow(
                    new ResourceAccessException(
                        "Cloud connection refused"
                    )
                );

        when(edge.predict(any()))
                .thenReturn(prediction());

        OrchestrationResult result =
                orchestrator.execute(task, "EDGE");

        assertEquals("SUCCESS", result.status());
        assertEquals("CLOUD", result.selectedRoute());
        assertEquals("EDGE", result.actualRoute());
        assertTrue(result.fallbackUsed());

        verify(cloud).predict(any());
        verify(edge).predict(any());
    }

    @Test
    void shouldNotFallbackWhenEdgeIsInfeasible() {

        when(decisionClient.evaluate(any()))
                .thenReturn(decision("CLOUD", false));

        when(cloud.predict(any()))
                .thenThrow(
                    new ResourceAccessException(
                        "Cloud connection refused"
                    )
                );

        assertThrows(
                ResourceAccessException.class,
                () -> orchestrator.execute(task, "EDGE")
        );

        verify(edge, never()).predict(any());
    }

    @Test
    void shouldHandleNoFeasibleRoute() {

        when(decisionClient.evaluate(any()))
                .thenReturn(
                    new DecisionResponse(
                        "TASK-001",
                        "NO_FEASIBLE_ROUTE",
                        null,
                        2.0,
                        List.of(),
                        List.of("No feasible execution route")
                    )
                );

        OrchestrationResult result =
                orchestrator.execute(task, "EDGE");

        assertEquals("NO_FEASIBLE_ROUTE",
                result.status());
        assertNull(result.actualRoute());
        assertNull(result.prediction());

        verify(edge, never()).predict(any());
        verify(cloud, never()).predict(any());
    }
    
@Test
void shouldExecuteHybridStepsInCorrectOrder() {

    when(decisionClient.evaluate(any()))
            .thenReturn(decision("HYBRID", true));

    HybridPreprocessResponse prepared =
            new HybridPreprocessResponse(
                    "TASK-001",
                    List.of(-0.90, 1.02, -1.34, -1.28),
                    1.8
            );

    HybridInferenceResult cloudResult =
            new HybridInferenceResult(
                    "TASK-001",
                    0,
                    0.94,
                    10.1
            );

    when(edge.hybridPreprocess(any()))
            .thenReturn(prepared);

    when(cloud.hybridInfer(any()))
            .thenReturn(cloudResult);

    when(edge.hybridPostprocess(any()))
            .thenReturn(prediction());

    OrchestrationResult result =
            orchestrator.execute(task, "EDGE");

    assertEquals("SUCCESS", result.status());
    assertEquals("HYBRID", result.selectedRoute());
    assertEquals("HYBRID", result.actualRoute());
    assertFalse(result.fallbackUsed());
    assertEquals(
            "SETOSA",
            result.prediction().prediction()
    );

    InOrder executionOrder = inOrder(edge, cloud);

    executionOrder.verify(edge)
            .hybridPreprocess(any());

    executionOrder.verify(cloud)
            .hybridInfer(any());

    executionOrder.verify(edge)
            .hybridPostprocess(any());
}

}
