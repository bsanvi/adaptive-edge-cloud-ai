
package com.adaptive.decision;

import com.adaptive.decision.dto.EstimatorResponse;
import com.adaptive.decision.dto.RouteEstimate;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstimatorResponseTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void deserializesAvailableEdgeEstimate()
            throws Exception {

        String json = """
            {
              "requestId": "task-001",
              "calibrationVersion": "test-v1",
              "calibrationSource": "ASSUMED",
              "estimates": {
                "EDGE": {
                  "estimateAvailable": true,
                  "reason": null,
                  "estimatedLatencyMs": 600.0,
                  "energyEstimateJ": 12.0,
                  "deviceEnergyEstimateJ": 12.0,
                  "cloudEnergyEstimateJ": 0.0,
                  "transferEnergyEstimateJ": 0.0,
                  "carbonEstimateG": 0.002,
                  "uploadBytes": 0,
                  "downloadBytes": 0,
                  "communicationMb": 0.0,
                  "resourcePressure": 0.4,
                  "source": "ESTIMATED"
                }
              }
            }
            """;

        EstimatorResponse response = mapper.readValue(
            json,
            EstimatorResponse.class
        );

        RouteEstimate edge = response.estimates().get("EDGE");

        assertNotNull(edge);
        assertTrue(edge.estimateAvailable());
        assertEquals(600.0, edge.estimatedLatencyMs());
        assertEquals(12.0, edge.energyEstimateJ());
    }

    @Test
    void handlesUnavailableRoute() throws Exception {
        String json = """
            {
              "requestId": "task-002",
              "calibrationVersion": "test-v1",
              "calibrationSource": "ASSUMED",
              "estimates": {
                "CLOUD": {
                  "estimateAvailable": false,
                  "reason": "NETWORK_UNAVAILABLE",
                  "estimatedLatencyMs": null,
                  "energyEstimateJ": null,
                  "deviceEnergyEstimateJ": null,
                  "cloudEnergyEstimateJ": null,
                  "transferEnergyEstimateJ": null,
                  "carbonEstimateG": null,
                  "uploadBytes": null,
                  "downloadBytes": null,
                  "communicationMb": null,
                  "resourcePressure": null,
                  "source": "ESTIMATED"
                }
              }
            }
            """;

        EstimatorResponse response = mapper.readValue(
            json,
            EstimatorResponse.class
        );

        RouteEstimate cloud = response.estimates().get("CLOUD");

        assertNotNull(cloud);
        assertFalse(cloud.estimateAvailable());
        assertEquals("NETWORK_UNAVAILABLE", cloud.reason());
        assertNull(cloud.estimatedLatencyMs());
    }
}
