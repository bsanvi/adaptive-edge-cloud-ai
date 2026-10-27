
package com.adaptive.decision;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class DecisionControllerTests {

    @LocalServerPort
    int port;

    @Test
    void returns503WhenSupportingInputsAreNotConfigured()
            throws Exception {

        String json = """
            {
              "task": {
                "taskId": "task-001",
                "taskType": "SENSOR_ANOMALY",
                "inputDataSizeBytes": 1024,
                "complexity": "LOW",
                "deadlineMs": 1000.0,
                "privacyLevel": "PRIVATE",
                "priorityProfile": "BALANCED"
              },
              "device": {
                "timestamp": "2026-10-27T06:00:00Z",
                "cpuPercent": 30.0,
                "ramPercent": 40.0,
                "batteryPercent": 80.0,
                "batteryAvailable": true
              },
              "network": {
                "connected": true,
                "latencyMs": 20.0,
                "bandwidthMbps": 50.0
              },
              "currentRoute": null
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(
                "http://localhost:" + port +
                "/api/v1/decision/evaluate"
            ))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(503, response.statusCode());
            assertTrue(response.body().contains("SERVICE_UNAVAILABLE"));
        }
    }
}
