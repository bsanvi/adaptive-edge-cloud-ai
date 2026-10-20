
package com.team.aecai.orchestrator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DemoControllerTest {

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new DemoController())
            .build();

    private final String requestJson = """
        {
          "taskId": "TASK-001",
          "features": [5.1, 3.5, 1.4, 0.2],
          "deadlineMs": 500,
          "privacyLevel": "NORMAL",
          "priorityProfile": "BALANCED",
          "requestedComplexity": "MEDIUM"
        }
        """;

    @Test
    void shouldSimulateHybridExecution() throws Exception {

        mvc.perform(post("/api/v1/demo/execute")
                .param("route", "HYBRID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status")
                    .value("SIMULATED"))
            .andExpect(jsonPath("$.actualRoute")
                    .value("HYBRID"))
            .andExpect(jsonPath("$.prediction.prediction")
                    .value("SETOSA"));
    }

    @Test
    void shouldRejectInvalidRoute() throws Exception {

        mvc.perform(post("/api/v1/demo/execute")
                .param("route", "INVALID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }
}
