package com.edgecloud.edge.controller;

import com.edgecloud.edge.dto.DemoMetricsRequest;
import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.service.DemoMetricsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DemoMetricsControllerTest {

    private MockMvc mockMvc;
    private DemoMetricsService demoMetricsService;

    @BeforeEach
    void setUp() {

        demoMetricsService =
                mock(DemoMetricsService.class);

        DemoMetricsController controller =
                new DemoMetricsController(
                        demoMetricsService
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void shouldSetControlledDemoMetrics()
            throws Exception {

        EdgeMetrics metrics =
                new EdgeMetrics(
                        90.0,
                        80.0,
                        13000,
                        3000,
                        15.0,
                        false,
                        "demo-time"
                );

        when(
                demoMetricsService.setDemoMetrics(
                        any(DemoMetricsRequest.class)
                )
        ).thenReturn(metrics);

        mockMvc.perform(
                        post("/api/v1/edge/demo/metrics")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "cpu_percent": 90,
                                          "ram_percent": 80,
                                          "ram_used_mb": 13000,
                                          "ram_available_mb": 3000,
                                          "battery_percent": 15,
                                          "power_plugged": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.measurement_type")
                                .value("simulated")
                )
                .andExpect(
                        jsonPath("$.metrics.cpu_percent")
                                .value(90.0)
                )
                .andExpect(
                        jsonPath("$.metrics.battery_percent")
                                .value(15.0)
                );
    }

    @Test
    void shouldReturnActiveDemoMetrics()
            throws Exception {

        EdgeMetrics metrics =
                new EdgeMetrics(
                        75.0,
                        65.0,
                        10000,
                        6000,
                        40.0,
                        true,
                        "demo-time"
                );

        when(
                demoMetricsService.isDemoMetricsActive()
        ).thenReturn(true);

        when(
                demoMetricsService.getDemoMetrics()
        ).thenReturn(metrics);

        mockMvc.perform(
                        get("/api/v1/edge/demo/metrics")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.measurement_type")
                                .value("simulated")
                )
                .andExpect(
                        jsonPath("$.metrics.cpu_percent")
                                .value(75.0)
                );
    }

    @Test
    void shouldClearDemoMetrics()
            throws Exception {

        mockMvc.perform(
                        delete("/api/v1/edge/demo/metrics")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.active")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.measurement_type")
                                .value("simulated")
                )
                .andExpect(
                        jsonPath("$.metrics")
                                .doesNotExist()
                );

        verify(
                demoMetricsService
        ).clearDemoMetrics();
    }
}
