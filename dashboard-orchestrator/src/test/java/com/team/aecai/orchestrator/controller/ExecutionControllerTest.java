
package com.team.aecai.orchestrator.controller;

import com.team.aecai.orchestrator.dto.*;
import com.team.aecai.orchestrator.orchestration.AdaptiveOrchestrator;
import com.team.aecai.orchestrator.repository.ExecutionHistoryRepository;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExecutionControllerTest {

    @Test
    void shouldExecuteAndSaveHistory() throws Exception {

        AdaptiveOrchestrator orchestrator =
                mock(AdaptiveOrchestrator.class);

        ExecutionHistoryRepository history =
                mock(ExecutionHistoryRepository.class);

        ExecutionController controller =
                new ExecutionController(orchestrator, history);

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
                                "TASK-001", "SETOSA", 0.94, 12.0
                        ),
                        null,
                        20.0,
                        "Task executed successfully"
                );

        when(orchestrator.execute(
                any(TaskRequest.class), eq("EDGE")
        )).thenReturn(result);

        String json = """
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
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.actualRoute").value("EDGE"))
            .andExpect(jsonPath("$.prediction.prediction")
                    .value("SETOSA"));

        verify(history).save(result);
    }

    @Test
    void shouldReturnExecutionHistory() throws Exception {

        AdaptiveOrchestrator orchestrator =
                mock(AdaptiveOrchestrator.class);

        ExecutionHistoryRepository history =
                mock(ExecutionHistoryRepository.class);

        ExecutionController controller =
                new ExecutionController(orchestrator, history);

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        when(history.findRecent(10)).thenReturn(
                List.of(
                        new ExecutionHistoryEntry(
                                1L,
                                "TASK-001",
                                "SUCCESS",
                                "EDGE",
                                "EDGE",
                                false,
                                "SETOSA",
                                0.94,
                                20.0,
                                "Task executed successfully",
                                "2026-10-20T12:00:00Z"
                        )
                )
        );

        mvc.perform(get("/api/v1/tasks/history")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].taskId")
                    .value("TASK-001"))
            .andExpect(jsonPath("$[0].prediction")
                    .value("SETOSA"));

        verify(history).findRecent(10);
    }
}
