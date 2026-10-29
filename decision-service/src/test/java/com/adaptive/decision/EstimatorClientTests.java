
package com.adaptive.decision;

import com.adaptive.decision.client.EstimatorClient;
import com.adaptive.decision.dto.EstimatorResponse;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

import com.adaptive.decision.dto.EstimatorRequest;

class EstimatorClientTests {

    @Test
    void sendsRequestAndReadsEstimatorResponse()
            throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0),
            0
        );

        server.createContext(
            "/api/v1/estimate/routes",
            exchange -> {

                byte[] response = """
                    {
                      "requestId": "task-001",
                      "calibrationVersion": "test-v1",
                      "calibrationSource": "ASSUMED",
                      "estimates": {}
                    }
                    """.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add(
                    "Content-Type",
                    "application/json"
                );

                exchange.sendResponseHeaders(
                    200,
                    response.length
                );

                try (var output =
                        exchange.getResponseBody()) {
                    output.write(response);
                }
            }
        );

        server.start();

        try {
            String baseUrl =
                "http://127.0.0.1:" +
                server.getAddress().getPort();

            EstimatorClient client = new EstimatorClient(
                RestClient.builder(),
                baseUrl
            );

            
EstimatorRequest request = new EstimatorRequest(
    "task-001",
    new EstimatorRequest.Task(
        "task-001",
        "SENSOR_ANOMALY",
        "LOW",
        1024L,
        256L,
        128L
    ),
    new EstimatorRequest.Device(
        30.0,
        40.0,
        80.0,
        "SIMULATED"
    ),
    new EstimatorRequest.Network(
        true,
        50.0,
        20.0,
        java.time.Instant.now(),
        "SIMULATED"
    ),
    true,
    true,
    1000.0,
    500.0,
    400.0
);

EstimatorResponse response = client.estimate(request);


            assertNotNull(response);
            assertEquals(
                "task-001",
                response.requestId()
            );

        } finally {
            server.stop(0);
        }
    }
}

