package com.edgecloud.edge.controller;

import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.dto.EdgeTaskRequest;
import com.edgecloud.edge.dto.EdgeTaskResponse;
import com.edgecloud.edge.dto.ImageResizeResult;
import com.edgecloud.edge.service.EdgeTaskService;
import com.edgecloud.edge.service.SystemMonitorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EdgeControllerTest {

    private MockMvc mockMvc;

    private SystemMonitorService systemMonitorService;
    private EdgeTaskService edgeTaskService;

    @BeforeEach
    void setUp() {

        systemMonitorService =
                mock(SystemMonitorService.class);

        edgeTaskService =
                mock(EdgeTaskService.class);

        EdgeController controller =
                new EdgeController(
                        systemMonitorService,
                        edgeTaskService
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void healthEndpointShouldReturnOk() throws Exception {

        mockMvc.perform(
                        get("/health")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ok")
                )
                .andExpect(
                        jsonPath("$.component")
                                .value("edge")
                );
    }

    @Test
    void metricsEndpointShouldReturnMetrics() throws Exception {

        EdgeMetrics metrics =
                new EdgeMetrics(
                        15.5,
                        70.0,
                        11000,
                        5000,
                        75.0,
                        true,
                        "test-time"
                );

        when(systemMonitorService.getEdgeMetrics())
                .thenReturn(metrics);

        mockMvc.perform(
                        get("/edge/metrics")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.cpu_percent")
                                .value(15.5)
                )
                .andExpect(
                        jsonPath("$.ram_percent")
                                .value(70.0)
                )
                .andExpect(
                        jsonPath("$.battery_percent")
                                .value(75.0)
                )
                .andExpect(
                        jsonPath("$.power_plugged")
                                .value(true)
                );
    }

    @Test
    void executeEndpointShouldReturnSuccessfulTask() throws Exception {

        EdgeTaskRequest request =
                new EdgeTaskRequest(
                        "task_001",
                        "input.jpg",
                        "output.jpg",
                        640,
                        480
                );

        EdgeMetrics metrics =
                new EdgeMetrics(
                        10.0,
                        60.0,
                        9000,
                        7000,
                        80.0,
                        true,
                        "test-time"
                );

        ImageResizeResult result =
                new ImageResizeResult(
                        "image_resize",
                        "input.jpg",
                        "output.jpg",
                        1200,
                        800,
                        640,
                        480,
                        15000,
                        5000,
                        66.67,
                        8.5,
                        "success"
                );

        EdgeTaskResponse response =
                new EdgeTaskResponse(
                        "task_001",
                        "edge",
                        "success",
                        "image_resize",
                        request,
                        8.5,
                        metrics,
                        metrics,
                        result,
                        null
                );

        when(
                edgeTaskService.runEdgeTask(
                        any(EdgeTaskRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/edge/execute")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "task_id": "task_001",
                                          "input_path": "input.jpg",
                                          "output_path": "output.jpg",
                                          "width": 640,
                                          "height": 480
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.task_id")
                                .value("task_001")
                )
                .andExpect(
                        jsonPath("$.execution_location")
                                .value("edge")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                )
                .andExpect(
                        jsonPath("$.task_type")
                                .value("image_resize")
                )
                .andExpect(
                        jsonPath("$.result.output_width")
                                .value(640)
                )
                .andExpect(
                        jsonPath("$.result.output_height")
                                .value(480)
                )
                .andExpect(
                        jsonPath("$.error")
                                .doesNotExist()
                );
    }
}