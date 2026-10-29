package com.edgecloud.edge.research;

import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.service.SystemMonitorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResearchEdgeControllerTest {

    private MockMvc mockMvc;

    private ImageModel imageModel;
    private SystemMonitorService systemMonitorService;

    @BeforeEach
    void setUp() {

        imageModel =
                mock(ImageModel.class);

        systemMonitorService =
                mock(SystemMonitorService.class);

        ResearchEdgeController controller =
                new ResearchEdgeController(
                        imageModel,
                        systemMonitorService
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void readyShouldReturnModelCapability()
            throws Exception {

        Research.Capability capability =
                new Research.Capability(
                        true,
                        "squeezenet1.1-7-image-v2",
                        "model-sha",
                        "labels-sha",
                        "rgb-stretch224-bilinear-imagenet-v1",
                        "edge-test-environment",
                        Map.of(
                                "os",
                                "test"
                        )
                );

        when(
                imageModel.capability()
        ).thenReturn(capability);

        mockMvc.perform(
                        get("/api/v2/edge/ready")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.ready")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.modelVersion")
                                .value(
                                        "squeezenet1.1-7-image-v2"
                                )
                )
                .andExpect(
                        jsonPath("$.preprocessingVersion")
                                .value(
                                        "rgb-stretch224-bilinear-imagenet-v1"
                                )
                )
                .andExpect(
                        jsonPath("$.environmentId")
                                .value(
                                        "edge-test-environment"
                                )
                );
    }

    @Test
    void telemetryShouldReturnLiveOshiMetrics()
            throws Exception {

        EdgeMetrics metrics =
                new EdgeMetrics(
                        25.5,
                        60.0,
                        9000,
                        7000,
                        75.0,
                        true,
                        "2026-10-29T10:00:00Z"
                );

        Research.Capability capability =
                new Research.Capability(
                        true,
                        "squeezenet1.1-7-image-v2",
                        "model-sha",
                        "labels-sha",
                        "rgb-stretch224-bilinear-imagenet-v1",
                        "edge-test-environment",
                        Map.of()
                );

        when(
                systemMonitorService.getEdgeMetrics()
        ).thenReturn(metrics);

        when(
                imageModel.capability()
        ).thenReturn(capability);

        mockMvc.perform(
                        get("/api/v2/edge/telemetry")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.cpuPercent")
                                .value(25.5)
                )
                .andExpect(
                        jsonPath("$.ramPercent")
                                .value(60.0)
                )
                .andExpect(
                        jsonPath("$.batteryPercent")
                                .value(75.0)
                )
                .andExpect(
                        jsonPath("$.powerPlugged")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.source")
                                .value(
                                        "LIVE_MEASUREMENT"
                                )
                )
                .andExpect(
                        jsonPath("$.environmentId")
                                .value(
                                        "edge-test-environment"
                                )
                );
    }
}