
package com.adaptive.decision;

import com.adaptive.decision.client.EstimatorClient;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.EstimationGateway;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EstimationGatewayTests {

    private DecisionRequest request() {
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

    private EstimatorInputs inputs() {
        return new EstimatorInputs(
            256L, 128L,
            true, true,
            500.0, 400.0,
            Instant.now(),
            "SIMULATED",
            "SIMULATED"
        );
    }

    @Test
    void adaptsRequestAndReturnsEstimates() {
        EstimatorClient client = mock(EstimatorClient.class);

        EstimatorResponse expected =
            new EstimatorResponse(
                "task-001",
                "test-v1",
                "ASSUMED",
                Map.of()
            );

        when(client.estimate(any(EstimatorRequest.class)))
            .thenReturn(expected);

        EstimationGateway gateway =
            new EstimationGateway(client);

        EstimatorResponse result =
            gateway.estimate(request(), inputs());

        assertSame(expected, result);
        verify(client).estimate(any(EstimatorRequest.class));
    }

    @Test
    void rejectsMismatchedRequestId() {
        EstimatorClient client = mock(EstimatorClient.class);

        when(client.estimate(any(EstimatorRequest.class)))
            .thenReturn(new EstimatorResponse(
                "wrong-task",
                "test-v1",
                "ASSUMED",
                Map.of()
            ));

        EstimationGateway gateway =
            new EstimationGateway(client);

        assertThrows(
            IllegalStateException.class,
            () -> gateway.estimate(request(), inputs())
        );
    }

    @Test
    void rejectsMissingResponse() {
        EstimatorClient client = mock(EstimatorClient.class);

        when(client.estimate(any(EstimatorRequest.class)))
            .thenReturn(null);

        EstimationGateway gateway =
            new EstimationGateway(client);

        assertThrows(
            IllegalStateException.class,
            () -> gateway.estimate(request(), inputs())
        );
    }

    @Test
    void rejectsMissingEstimateMap() {
        EstimatorClient client = mock(EstimatorClient.class);

        when(client.estimate(any(EstimatorRequest.class)))
            .thenReturn(new EstimatorResponse(
                "task-001",
                "test-v1",
                "ASSUMED",
                null
            ));

        EstimationGateway gateway =
            new EstimationGateway(client);

        assertThrows(
            IllegalStateException.class,
            () -> gateway.estimate(request(), inputs())
        );
    }
}
