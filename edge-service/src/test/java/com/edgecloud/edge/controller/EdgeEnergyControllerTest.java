package com.edgecloud.edge.controller;

import com.edgecloud.edge.dto.EdgeEnergyEstimateRequest;
import com.edgecloud.edge.dto.EdgeEnergyEstimateResult;
import com.edgecloud.edge.service.EdgeEnergyEstimatorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EdgeEnergyControllerTest {

    private MockMvc mockMvc;
    private EdgeEnergyEstimatorService edgeEnergyEstimatorService;

    @BeforeEach
    void setUp() {

        edgeEnergyEstimatorService =
                mock(EdgeEnergyEstimatorService.class);

        EdgeEnergyController controller =
                new EdgeEnergyController(
                        edgeEnergyEstimatorService
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void energyEstimateEndpointShouldReturnEstimate()
            throws Exception {

        EdgeEnergyEstimateResult result =
                new EdgeEnergyEstimateResult(
                        "TASK-301",
                        30.0,
                        100.0,
                        12.5,
                        1.25,
                        "estimated",
                        "success"
                );

        when(
                edgeEnergyEstimatorService.estimateEnergy(
                        any(EdgeEnergyEstimateRequest.class)
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/edge/energy/estimate")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "task_id": "TASK-301",
                                          "execution_time_ms": 100,
                                          "cpu_percent_before": 20,
                                          "cpu_percent_after": 40
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.task_id")
                                .value("TASK-301")
                )
                .andExpect(
                        jsonPath("$.average_cpu_percent")
                                .value(30.0)
                )
                .andExpect(
                        jsonPath("$.estimated_power_watts")
                                .value(12.5)
                )
                .andExpect(
                        jsonPath("$.estimated_energy_joules")
                                .value(1.25)
                )
                .andExpect(
                        jsonPath("$.measurement_type")
                                .value("estimated")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }
}
