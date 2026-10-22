
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DecisionApiClientTest {

    @Test
    void shouldEvaluateDecision() {

        // Step 1: Create mock HTTP server
        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        // Step 2: Create Decision client
        DecisionApiClient client = new DecisionApiClient(
                builder,
                "http://localhost:8082"
        );

        // Step 3: Create task profile
        TaskProfile task = new TaskProfile(
                "TASK-001",
                "IRIS_CLASSIFICATION",
                32,
                "HIGH",
                500,
                "NORMAL",
                "REAL_TIME"
        );

        // Step 4: Simulated device metrics
        DeviceMetrics device = new DeviceMetrics(
                Instant.parse("2026-10-20T11:00:00Z"),
                88.0,
                61.0,
                44.0,
                true
        );

        // Step 5: Simulated network metrics
        NetworkMetrics network = new NetworkMetrics(
                true,
                20.0,
                80.0
        );

        // Step 6: Prepare Decision request
        DecisionRequest request = new DecisionRequest(
                task,
                device,
                network,
                "EDGE"
        );

        // Step 7: Simulated Decision Engine response
        String responseJson = """
            {
              "taskId": "TASK-001",
              "status": "ROUTE_SELECTED",
              "selectedRoute": "CLOUD",
              "decisionTimeMs": 2.4,
              "evaluations": [
                {
                  "route": "EDGE",
                  "feasible": true,
                  "estimatedLatencyMs": 420.0,
                  "estimatedEnergyJ": 8.3,
                  "estimatedCarbonG": 0.0012,
                  "communicationCost": 0.0,
                  "switchingCost": 0.0,
                  "score": 0.68,
                  "rejectionReasons": []
                },
                {
                  "route": "CLOUD",
                  "feasible": true,
                  "estimatedLatencyMs": 170.0,
                  "estimatedEnergyJ": 3.4,
                  "estimatedCarbonG": 0.0018,
                  "communicationCost": 0.08,
                  "switchingCost": 0.05,
                  "score": 0.39,
                  "rejectionReasons": []
                }
              ],
              "reasons": [
                "Cloud has the lowest feasible score",
                "Cloud meets the deadline"
              ]
            }
            """;

        // Step 8: Configure the mock HTTP request
        server.expect(requestTo(
                "http://localhost:8082/api/v1/decision/evaluate"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.task.taskId")
                    .value("TASK-001"))
            .andExpect(jsonPath("$.device.cpuPercent")
                    .value(88.0))
            .andExpect(jsonPath("$.network.latencyMs")
                    .value(20.0))
            .andExpect(jsonPath("$.currentRoute")
                    .value("EDGE"))
            .andRespond(withSuccess(
                    responseJson,
                    MediaType.APPLICATION_JSON
            ));

        // Step 9: Call the actual client
        DecisionResponse response = client.evaluate(request);

        // Step 10: Verify response
        assertNotNull(response);
        assertEquals("TASK-001", response.taskId());
        assertEquals("ROUTE_SELECTED", response.status());
        assertEquals("CLOUD", response.selectedRoute());
        assertEquals(2.4, response.decisionTimeMs());

        assertEquals(2, response.evaluations().size());
        assertEquals(
                0.39,
                response.evaluations().get(1).score()
        );

        assertFalse(response.reasons().isEmpty());

        server.verify();
    }
}
