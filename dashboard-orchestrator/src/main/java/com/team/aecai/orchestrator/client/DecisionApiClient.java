
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.DecisionRequest;
import com.team.aecai.orchestrator.dto.DecisionResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DecisionApiClient {

    private final RestClient restClient;

    public DecisionApiClient(
            RestClient.Builder builder,
            @Value("${services.decision.base-url}")
            String decisionBaseUrl) {

        this.restClient = builder
                .baseUrl(decisionBaseUrl)
                .build();
    }

    public DecisionResponse evaluate(
            DecisionRequest request) {

        return restClient.post()
                .uri("/api/v1/decision/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DecisionResponse.class);
    }
}
