
package com.team.aecai.orchestrator.client;

import com.team.aecai.orchestrator.dto.TaskRequest;
import com.team.aecai.orchestrator.dto.TaskProfile;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EdgeTaskProfileTest {

    @Test
    void shouldProfileTask() {

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

        // Create sample task
        TaskRequest request = new TaskRequest(
                "TASK-001",
                List.of(5.1, 3.5, 1.4, 0.2),
                500,
                "NORMAL",
                "BALANCED",
                "MEDIUM"
        );

        // Simulated Edge task profile response
        String responseJson = """
            {
              "taskId": "TASK-001",
              "taskType": "IRIS_CLASSIFICATION",
              "inputDataSizeBytes": 32,
              "complexity": "MEDIUM",
              "deadlineMs": 500,
              "privacyLevel": "NORMAL",
              "priorityProfile": "BALANCED"
            }
            """;

        // Configure mock API
        server.expect(requestTo(
                "http://localhost:8081/api/v1/edge/profile"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.taskId").value("TASK-001"))
            .andExpect(jsonPath("$.features.length()").value(4))
            .andRespond(withSuccess(
                responseJson,
                MediaType.APPLICATION_JSON
            ));

        // Call the API
        TaskProfile profile = client.profileTask(request);

        // Verify returned task profile
        assertNotNull(profile);
        assertEquals("TASK-001", profile.taskId());
        assertEquals("IRIS_CLASSIFICATION", profile.taskType());
        assertEquals(32, profile.inputDataSizeBytes());
        assertEquals("MEDIUM", profile.complexity());
        assertEquals(500.0, profile.deadlineMs());
        assertEquals("NORMAL", profile.privacyLevel());
        assertEquals("BALANCED", profile.priorityProfile());

        // Verify HTTP interaction
        server.verify();
    }
}
