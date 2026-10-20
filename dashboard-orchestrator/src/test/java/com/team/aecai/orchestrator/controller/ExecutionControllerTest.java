
package com.team.aecai.orchestrator.controller;

import com.team.aecai.orchestrator.dto.*;
import com.team.aecai.orchestrator.orchestration.AdaptiveOrchestrator;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExecutionControllerTest {

    @Test
    void shouldAcceptTaskExecutionRequest() throws Exception {

        AdaptiveOrchestrator orchestrator =
                mock(AdaptiveOrchestrator.class);

        ExecutionController controller =
                new ExecutionController(orchestrator);

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        OrchestrationResult result =
                new OrchestrationResult(
                        "TASK-001",
                        "SUCCESS",
                        "EDGE",
                        "EDGE",
                        false,
                        new InferenceResponse(
                                "TASK-001",
                                "SETOSA",
                                0.94,
                                12.0
                        ),
                        null,
                        20.0,
                        "Task executed successfully"
                );

        when(orchestrator.execute(
                any(TaskRequest.class),
                eq("EDGE")
        )).thenReturn(result);

        String requestJson = """
            {
              "taskId": "TASK-001",
              "features": [5.1, 3.5, 1.4, 0.2],
              "deadlineMs": 500,
              "privacyLevel": "NORMAL",
              "priorityProfile": "BALANCED",
              "requestedComplexity": "MEDIUM"
            }
            """;

        mvc.perform(post("/api/v1/tasks/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status")
                    .value("SUCCESS"))
            .andExpect(jsonPath("$.actualRoute")
                    .value("EDGE"))
            .andExpect(jsonPath("$.prediction.prediction")
                    .value("SETOSA"));

        verify(orchestrator).execute(
                any(TaskRequest.class),
                eq("EDGE")
        );
    }
}
