
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudApiClientTest {

    @Test
    void shouldCheckCloudHealth() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        CloudApiClient client = new CloudApiClient(
                builder,
                "http://localhost:8083"
        );

        server.expect(requestTo(
                "http://localhost:8083/api/v1/cloud/health"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                "{\"status\":\"online\"}",
                MediaType.APPLICATION_JSON
            ));

        CloudHealthResponse response = client.getHealth();

        assertNotNull(response);
        assertEquals("online", response.status());

        server.verify();
    }

    @Test
    void shouldExecuteCloudPrediction() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        CloudApiClient client = new CloudApiClient(
                builder,
                "http://localhost:8083"
        );

        InferenceRequest request = new InferenceRequest(
                "TASK-001",
                List.of(5.1, 3.5, 1.4, 0.2)
        );

        String responseJson = """
            {
              "taskId": "TASK-001",
              "prediction": "SETOSA",
              "confidence": 0.94,
              "inferenceTimeMs": 15.6
            }
            """;

        server.expect(requestTo(
                "http://localhost:8083/api/v1/cloud/predict"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.taskId").value("TASK-001"))
            .andExpect(jsonPath("$.features.length()").value(4))
            .andRespond(withSuccess(
                    responseJson,
                    MediaType.APPLICATION_JSON
            ));

        InferenceResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals("TASK-001", response.taskId());
        assertEquals("SETOSA", response.prediction());
        assertEquals(0.94, response.confidence());
        assertEquals(15.6, response.inferenceTimeMs());

        server.verify();
    }
}
