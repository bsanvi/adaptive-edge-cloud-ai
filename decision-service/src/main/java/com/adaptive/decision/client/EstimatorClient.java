
package com.adaptive.decision.client;

import com.adaptive.decision.dto.EstimatorRequest;
import com.adaptive.decision.dto.EstimatorResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EstimatorClient {

    private final RestClient restClient;

    public EstimatorClient(
            RestClient.Builder builder,
            @Value("${services.estimator.base-url}")
            String estimatorBaseUrl) {

        this.restClient = builder
                .baseUrl(estimatorBaseUrl)
                .build();
    }

    public EstimatorResponse estimate(
            EstimatorRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                "Estimator request cannot be null"
            );
        }

        return restClient.post()
                .uri("/api/v1/estimate/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EstimatorResponse.class);
    }
}
