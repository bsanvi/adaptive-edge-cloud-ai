
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

    public EstimatorClient(RestClient.Builder builder, String estimatorBaseUrl) {
        this(builder, estimatorBaseUrl, 2000, 5000);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EstimatorClient(
            RestClient.Builder builder,
            @Value("${services.estimator.base-url}")
            String estimatorBaseUrl,
            @Value("${services.estimator.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${services.estimator.read-timeout-ms:5000}") int readTimeoutMs) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0)
            throw new IllegalArgumentException("Estimator timeouts must be positive");
        var http = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofMillis(connectTimeoutMs)).build();
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(java.time.Duration.ofMillis(readTimeoutMs));


        this.restClient = builder
                .requestFactory(factory)
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
