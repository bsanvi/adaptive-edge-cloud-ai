
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.HybridCloudRequest;
import com.team.aecai.orchestrator.dto.HybridInferenceResult;

import com.team.aecai.orchestrator.dto.CloudHealthResponse;
import com.team.aecai.orchestrator.dto.InferenceRequest;
import com.team.aecai.orchestrator.dto.InferenceResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CloudApiClient {

    private final RestClient restClient;

    public CloudApiClient(
            RestClient.Builder builder,
            @Value("${services.cloud.base-url}")
            String cloudBaseUrl) {

        this.restClient = builder
                .baseUrl(cloudBaseUrl)
                .build();
    }

    // Check whether the Cloud service responds
    public CloudHealthResponse getHealth() {

        return restClient.get()
                .uri("/api/v1/cloud/health")
                .retrieve()
                .body(CloudHealthResponse.class);
    }

    // Ask Cloud to perform an AI prediction
    public InferenceResponse predict(
            InferenceRequest request) {

        return restClient.post()
                .uri("/api/v1/cloud/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(InferenceResponse.class);
    }
    
    public HybridInferenceResult hybridInfer(
            HybridCloudRequest request) {
            
        return restClient.post()
                .uri("/api/v1/cloud/hybrid/infer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(HybridInferenceResult.class);
    }

}
