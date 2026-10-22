
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.InferenceRequest;
import com.team.aecai.orchestrator.dto.InferenceResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EdgePredictionTest {

    @Test
    void shouldExecuteEdgePrediction() {

        // Create HTTP client builder
        RestClient.Builder builder = RestClient.builder();

        // Create mock server
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        // Initialize Edge API client
        EdgeApiClient client = new EdgeApiClient(
                builder,
                "http://localhost:8081"
        );

        // Create prediction request
        InferenceRequest request = new InferenceRequest(
                "TASK-001",
                List.of(5.1, 3.5, 1.4, 0.2)
        );

        // Simulated AI prediction response
        String responseJson = """
            {
              "taskId": "TASK-001",
              "prediction": "SETOSA",
              "confidence": 0.94,
              "inferenceTimeMs": 12.7
            }
            """;

        // Configure mocked Edge prediction API
        server.expect(requestTo(
                "http://localhost:8081/api/v1/edge/predict"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.taskId").value("TASK-001"))
            .andExpect(jsonPath("$.features.length()").value(4))
            .andRespond(withSuccess(
                responseJson,
                MediaType.APPLICATION_JSON
            ));

        // Execute prediction request
        InferenceResponse response = client.predict(request);

        // Verify response
        assertNotNull(response);
        assertEquals("TASK-001", response.taskId());
        assertEquals("SETOSA", response.prediction());
        assertEquals(0.94, response.confidence());
        assertEquals(12.7, response.inferenceTimeMs());

        // Verify HTTP request
        server.verify();
    }
}

