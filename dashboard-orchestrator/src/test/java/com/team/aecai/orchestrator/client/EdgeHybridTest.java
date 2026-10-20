
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

class EdgeHybridTest {

    @Test
    void shouldPreprocessFeatures() {

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        EdgeApiClient client =
                new EdgeApiClient(builder, "http://localhost:8081");

        InferenceRequest request = new InferenceRequest(
                "TASK-001",
                List.of(5.1, 3.5, 1.4, 0.2)
        );

        String json = """
            {
              "taskId": "TASK-001",
              "processedFeatures": [-0.90, 1.02, -1.34, -1.28],
              "processingTimeMs": 1.8
            }
            """;

        server.expect(requestTo(
                "http://localhost:8081/api/v1/edge/hybrid/preprocess"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.taskId").value("TASK-001"))
            .andExpect(jsonPath("$.features.length()").value(4))
            .andRespond(withSuccess(
                    json, MediaType.APPLICATION_JSON));

        HybridPreprocessResponse response =
                client.hybridPreprocess(request);

        assertNotNull(response);
        assertEquals("TASK-001", response.taskId());
        assertEquals(4, response.processedFeatures().size());
        assertEquals(-0.90, response.processedFeatures().get(0));
        assertEquals(1.8, response.processingTimeMs());

        server.verify();
    }

    @Test
    void shouldPostprocessCloudResult() {

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        EdgeApiClient client =
                new EdgeApiClient(builder, "http://localhost:8081");

        HybridInferenceResult request =
                new HybridInferenceResult(
                        "TASK-001", 0, 0.94, 10.1
                );

        String json = """
            {
              "taskId": "TASK-001",
              "prediction": "SETOSA",
              "confidence": 0.94,
              "inferenceTimeMs": 1.2
            }
            """;

        server.expect(requestTo(
                "http://localhost:8081/api/v1/edge/hybrid/postprocess"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.classIndex").value(0))
            .andRespond(withSuccess(
                    json, MediaType.APPLICATION_JSON));

        InferenceResponse response =
                client.hybridPostprocess(request);

        assertNotNull(response);
        assertEquals("TASK-001", response.taskId());
        assertEquals("SETOSA", response.prediction());
        assertEquals(0.94, response.confidence());

        server.verify();
    }
}
