
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.DeviceMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

class EdgeApiClientTest {

    @Test
    void testGetMetrics() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

        EdgeApiClient client = new EdgeApiClient(
        builder,
        "http://localhost:8081"
        );
        

        String json = """
            {
              "timestamp": "2026-10-20T11:00:00Z",
              "cpuPercent": 45.2,
              "ramPercent": 62.5,
              "batteryPercent": 78.0,
              "batteryAvailable": true
            }
            """;

        server.expect(requestTo(
                "http://localhost:8081/api/v1/edge/metrics"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                json, MediaType.APPLICATION_JSON));

        DeviceMetrics metrics = client.getMetrics();

        assertNotNull(metrics);
        assertEquals(45.2, metrics.cpuPercent());
        assertEquals(62.5, metrics.ramPercent());
        assertEquals(78.0, metrics.batteryPercent());
        assertTrue(metrics.batteryAvailable());

        server.verify();
    }
}
