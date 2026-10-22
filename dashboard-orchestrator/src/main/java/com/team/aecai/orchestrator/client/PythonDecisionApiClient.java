
package com.team.aecai.orchestrator.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PythonDecisionApiClient {

    private final RestClient restClient;

    public PythonDecisionApiClient(
            RestClient.Builder builder,
            @Value("${services.decision.base-url}")
            String decisionBaseUrl) {

        this.restClient = builder
                .baseUrl(decisionBaseUrl)
                .build();
    }

    public Map<String, Object> evaluate(
            Map<String, Object> request) {

        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException(
                    "Decision request cannot be empty"
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/decision/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Decision Engine returned no response"
            );
        }

        return response;
    }
}
