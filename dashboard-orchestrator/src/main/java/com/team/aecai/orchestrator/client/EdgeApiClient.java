
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.DeviceMetrics;
import com.team.aecai.orchestrator.dto.TaskRequest;
import com.team.aecai.orchestrator.dto.TaskProfile;
import com.team.aecai.orchestrator.dto.InferenceRequest;
import com.team.aecai.orchestrator.dto.InferenceResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EdgeApiClient {

    private final RestClient restClient;

    public EdgeApiClient(
            RestClient.Builder builder,
            @Value("${services.edge.base-url}") String edgeBaseUrl) {

        this.restClient = builder
                .baseUrl(edgeBaseUrl)
                .build();
    }

    // Get CPU, RAM, and battery information
    public DeviceMetrics getMetrics() {

        return restClient.get()
                .uri("/api/v1/edge/metrics")
                .retrieve()
                .body(DeviceMetrics.class);
    }

    // Get task properties from the Edge service
    public TaskProfile profileTask(TaskRequest task) {

        return restClient.post()
                .uri("/api/v1/edge/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .body(task)
                .retrieve()
                .body(TaskProfile.class);
    }

    // Ask the Edge service to run an AI prediction
    public InferenceResponse predict(InferenceRequest request) {

        return restClient.post()
                .uri("/api/v1/edge/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(InferenceResponse.class);
    }
}
