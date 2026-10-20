
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.DeviceMetrics;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EdgeApiClient {

    private final RestClient restClient;

    public EdgeApiClient(RestClient.Builder builder) {
        this.restClient = builder
            .baseUrl("http://localhost:8081")
            .build();
    }

    public DeviceMetrics getMetrics() {
        return restClient.get()
            .uri("/api/v1/edge/metrics")
            .retrieve()
            .body(DeviceMetrics.class);
    }
}
