
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.DeviceMetrics;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EdgeApiClientTest {

    @Test
    void testGetMetrics() {

        // Step 1: Create RestClient builder
        RestClient.Builder builder = RestClient.builder();

        // Step 2: Create a mock HTTP server
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        // Step 3: Create EdgeApiClient
        EdgeApiClient client = new EdgeApiClient(
                builder,
                "http://localhost:8081"
        );

        // Step 4: Simulated response from Edge service
        String responseJson = """
            {
              "timestamp": "2026-10-20T11:00:00Z",
              "cpuPercent": 45.2,
              "ramPercent": 62.5,
              "batteryPercent": 78.0,
              "batteryAvailable": true
            }
            """;

        // Step 5: Define the expected HTTP request
        server.expect(requestTo(
                "http://localhost:8081/api/v1/edge/metrics"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                responseJson,
                MediaType.APPLICATION_JSON
            ));

        // Step 6: Call the actual client method
        DeviceMetrics metrics = client.getMetrics();

        // Step 7: Verify the response values
        assertNotNull(metrics);

        assertEquals(45.2, metrics.cpuPercent());
        assertEquals(62.5, metrics.ramPercent());
        assertEquals(78.0, metrics.batteryPercent());
        assertTrue(metrics.batteryAvailable());

        // Step 8: Verify that the expected API call occurred
        server.verify();
    }
}
