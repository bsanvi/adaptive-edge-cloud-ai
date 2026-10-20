
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.HybridCloudRequest;
import com.team.aecai.orchestrator.dto.HybridInferenceResult;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudHybridTest {

    @Test
    void shouldExecuteHybridCloudInference() {

        // Create HTTP client builder
        RestClient.Builder builder = RestClient.builder();

        // Mock Person 3's Cloud server
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        // Create our Cloud client
        CloudApiClient client = new CloudApiClient(
                builder,
                "http://localhost:8083"
        );

        // Features already processed by Edge
        HybridCloudRequest request = new HybridCloudRequest(
                "TASK-001",
                List.of(-0.90, 1.02, -1.34, -1.28)
        );

        // Simulated Cloud inference result
        String responseJson = """
            {
              "taskId": "TASK-001",
              "classIndex": 0,
              "confidence": 0.94,
              "inferenceTimeMs": 10.1
            }
            """;

        // Check the request and return mock result
        server.expect(requestTo(
                "http://localhost:8083/api/v1/cloud/hybrid/infer"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.taskId")
                    .value("TASK-001"))
            .andExpect(jsonPath("$.processedFeatures.length()")
                    .value(4))
            .andRespond(withSuccess(
                    responseJson,
                    MediaType.APPLICATION_JSON
            ));

        // Call our Cloud client
        HybridInferenceResult result =
                client.hybridInfer(request);

        // Verify returned values
        assertNotNull(result);
        assertEquals("TASK-001", result.taskId());
        assertEquals(0, result.classIndex());
        assertEquals(0.94, result.confidence());
        assertEquals(10.1, result.inferenceTimeMs());

        server.verify();
    }
}
