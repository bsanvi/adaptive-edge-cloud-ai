
package com.team.aecai.orchestrator.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PythonDecisionApiClientTest {

    @Test
    void shouldSendRequestAndReadPythonDecision() {

        // 1. Create a simulated HTTP server.
        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        // 2. Create our actual Spring Boot adapter.
        PythonDecisionApiClient client =
                new PythonDecisionApiClient(
                        builder,
                        "http://127.0.0.1:8001"
                );

        // 3. Create a request matching Python's API.
        Map<String, Object> task = Map.of(
                "task_id", "java-test-002",
                "task_type", "anomaly_detection",
                "data_size_mb", 1.0,
                "complexity", 0.5,
                "deadline_ms", 1000.0,
                "privacy_required", true
        );

        Map<String, Object> edge = Map.of(
                "cpu_percent", 30.0,
                "ram_percent", 40.0,
                "battery_percent", 80.0,
                "power_plugged", true
        );

        Map<String, Object> network = Map.of(
                "latency_ms", 20.0,
                "bandwidth_mbps", 50.0
        );

        Map<String, Object> edgeEstimate = Map.of(
                "estimated_time_ms", 100.0,
                "estimated_energy_j", 5.0,
                "estimated_cost", 0.0,
                "estimated_carbon_g", 0.1
        );

        Map<String, Object> request = Map.of(
                "task", task,
                "edge", edge,
                "network", network,
                "estimates", Map.of("edge", edgeEstimate)
        );

        // 4. Simulate Python's JSON response.
        String responseJson = """
            {
              "task_id": "java-test-002",
              "status": "selected",
              "selected_location": "edge",
              "explanation": "Edge selected because of privacy."
            }
            """;

        // 5. Verify correct URL, method and request body.
        server.expect(requestTo(
                "http://127.0.0.1:8001/decision/evaluate"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.task.task_id")
                    .value("java-test-002"))
            .andExpect(jsonPath("$.task.privacy_required")
                    .value(true))
            .andExpect(jsonPath("$.edge.cpu_percent")
                    .value(30.0))
            .andExpect(jsonPath("$.estimates.edge.estimated_time_ms")
                    .value(100.0))
            .andRespond(withSuccess(
                    responseJson,
                    MediaType.APPLICATION_JSON
            ));

        // 6. Call the Java adapter.
        Map<String, Object> response =
                client.evaluate(request);

        // 7. Check the response.
        assertNotNull(response);

        assertEquals(
                "java-test-002",
                response.get("task_id")
        );

        assertEquals(
                "selected",
                response.get("status")
        );

        assertEquals(
                "edge",
                response.get("selected_location")
        );

        server.verify();
    }
}
