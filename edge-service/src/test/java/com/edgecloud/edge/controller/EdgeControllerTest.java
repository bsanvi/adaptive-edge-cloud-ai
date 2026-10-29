package com.edgecloud.edge.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.dto.EdgeTaskRequest;
import com.edgecloud.edge.dto.EdgeTaskResponse;
import com.edgecloud.edge.dto.HybridPostprocessRequest;
import com.edgecloud.edge.dto.HybridPostprocessResult;
import com.edgecloud.edge.dto.HybridPreprocessRequest;
import com.edgecloud.edge.dto.HybridPreprocessResult;
import com.edgecloud.edge.dto.ImageResizeResult;
import com.edgecloud.edge.dto.MlInferenceResult;
import com.edgecloud.edge.dto.TaskProfileRequest;
import com.edgecloud.edge.dto.TaskProfileResult;
import com.edgecloud.edge.service.EdgeTaskService;
import com.edgecloud.edge.service.HybridPostprocessService;
import com.edgecloud.edge.service.HybridPreprocessService;
import com.edgecloud.edge.service.MlInferenceService;
import com.edgecloud.edge.service.SystemMonitorService;
import com.edgecloud.edge.service.TaskProfilerService;

class EdgeControllerTest {

    private MockMvc mockMvc;

    private SystemMonitorService systemMonitorService;
    private EdgeTaskService edgeTaskService;
    private MlInferenceService mlInferenceService;
    private HybridPreprocessService hybridPreprocessService;
    private HybridPostprocessService hybridPostprocessService;
    private TaskProfilerService taskProfilerService;

    @BeforeEach
    void setUp() {

        systemMonitorService =
                mock(SystemMonitorService.class);

        edgeTaskService =
                mock(EdgeTaskService.class);

        mlInferenceService =
                mock(MlInferenceService.class);

        hybridPreprocessService =
                mock(HybridPreprocessService.class);

        hybridPostprocessService =
                mock(HybridPostprocessService.class);

        taskProfilerService =
                mock(TaskProfilerService.class);

        EdgeController controller =
                new EdgeController(
                        systemMonitorService,
                        edgeTaskService,
                        mlInferenceService,
                        hybridPreprocessService,
                        hybridPostprocessService,
                        taskProfilerService
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
    void integrationHealthEndpointShouldReturnOk()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/edge/health")
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
    void metricsEndpointShouldReturnMetrics()
            throws Exception {

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

        when(
                systemMonitorService.getEdgeMetrics()
        ).thenReturn(metrics);

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
    void integrationMetricsEndpointShouldReturnMetrics()
            throws Exception {

        EdgeMetrics metrics =
                new EdgeMetrics(
                        20.0,
                        65.0,
                        10000,
                        6000,
                        70.0,
                        false,
                        "test-time"
                );

        when(
                systemMonitorService.getEdgeMetrics()
        ).thenReturn(metrics);

        mockMvc.perform(
                        get("/api/v1/edge/metrics")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.cpu_percent")
                                .value(20.0)
                )
                .andExpect(
                        jsonPath("$.ram_percent")
                                .value(65.0)
                )
                .andExpect(
                        jsonPath("$.battery_percent")
                                .value(70.0)
                );
    }

    @Test
    void executeEndpointShouldReturnSuccessfulTask()
            throws Exception {

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
                );
    }

    @Test
    void classifyEndpointShouldReturnPrediction()
            throws Exception {

        MlInferenceResult result =
                new MlInferenceResult(
                        "image_classification",
                        "image.jpg",
                        281,
                        "tabby",
                        0.82,
                        25.4,
                        "success"
                );

        when(
                mlInferenceService.classifyImage(
                        anyString()
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/edge/classify")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "input_path": "image.jpg"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.predicted_label")
                                .value("tabby")
                )
                .andExpect(
                        jsonPath("$.confidence")
                                .value(0.82)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }

    @Test
    void integrationPredictEndpointShouldReturnPrediction()
            throws Exception {

        MlInferenceResult result =
                new MlInferenceResult(
                        "image_classification",
                        "image.jpg",
                        281,
                        "tabby",
                        0.82,
                        25.4,
                        "success"
                );

        when(
                mlInferenceService.classifyImage(
                        anyString()
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/api/v1/edge/predict")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "input_path": "image.jpg"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.predicted_label")
                                .value("tabby")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }

    @Test
    void hybridPreprocessEndpointShouldReturnProcessedImage()
            throws Exception {

        HybridPreprocessResult result =
                new HybridPreprocessResult(
                        "TASK-101",
                        "hybrid_preprocess",
                        "input.jpg",
                        "processed.jpg",
                        1200,
                        800,
                        224,
                        224,
                        15000,
                        3000,
                        80.0,
                        5.25,
                        "success"
                );

        when(
                hybridPreprocessService.preprocess(
                        any(HybridPreprocessRequest.class)
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/edge/hybrid/preprocess")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "task_id": "TASK-101",
                                          "input_path": "input.jpg",
                                          "output_path": "processed.jpg"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.task_id")
                                .value("TASK-101")
                )
                .andExpect(
                        jsonPath("$.processed_width")
                                .value(224)
                )
                .andExpect(
                        jsonPath("$.processed_height")
                                .value(224)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }

    @Test
    void integrationHybridPreprocessEndpointShouldWork()
            throws Exception {

        HybridPreprocessResult result =
                new HybridPreprocessResult(
                        "TASK-101",
                        "hybrid_preprocess",
                        "input.jpg",
                        "processed.jpg",
                        1200,
                        800,
                        224,
                        224,
                        15000,
                        3000,
                        80.0,
                        5.25,
                        "success"
                );

        when(
                hybridPreprocessService.preprocess(
                        any(HybridPreprocessRequest.class)
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/api/v1/edge/hybrid/preprocess")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "task_id": "TASK-101",
                                          "input_path": "input.jpg",
                                          "output_path": "processed.jpg"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.processed_width")
                                .value(224)
                )
                .andExpect(
                        jsonPath("$.processed_height")
                                .value(224)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }

    @Test
    void hybridPostprocessEndpointShouldReturnFinalPrediction()
            throws Exception {

        HybridPostprocessResult result =
                new HybridPostprocessResult(
                        "TASK-101",
                        "hybrid_postprocess",
                        281,
                        "tabby",
                        0.82,
                        0.25,
                        "success"
                );

        when(
                hybridPostprocessService.postprocess(
                        any(HybridPostprocessRequest.class)
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/edge/hybrid/postprocess")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "task_id": "TASK-101",
                                          "class_index": 281,
                                          "confidence": 0.82
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.task_id")
                                .value("TASK-101")
                )
                .andExpect(
                        jsonPath("$.class_index")
                                .value(281)
                )
                .andExpect(
                        jsonPath("$.predicted_label")
                                .value("tabby")
                )
                .andExpect(
                        jsonPath("$.confidence")
                                .value(0.82)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }

    @Test
    void integrationHybridPostprocessEndpointShouldWork()
            throws Exception {

        HybridPostprocessResult result =
                new HybridPostprocessResult(
                        "TASK-101",
                        "hybrid_postprocess",
                        281,
                        "tabby",
                        0.82,
                        0.25,
                        "success"
                );

        when(
                hybridPostprocessService.postprocess(
                        any(HybridPostprocessRequest.class)
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/api/v1/edge/hybrid/postprocess")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "task_id": "TASK-101",
                                          "class_index": 281,
                                          "confidence": 0.82
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.predicted_label")
                                .value("tabby")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }

    @Test
    void profileEndpointShouldReturnTaskProfile()
            throws Exception {

        TaskProfileResult result =
                new TaskProfileResult(
                        "TASK-201",
                        "image_classification",
                        "input.jpg",
                        12000,
                        800,
                        600,
                        480000,
                        "MEDIUM",
                        500.0,
                        "NORMAL",
                        "BALANCED",
                        "success"
                );

        when(
                taskProfilerService.profileTask(
                        any(TaskProfileRequest.class)
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/edge/profile")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "task_id": "TASK-201",
                                          "input_path": "input.jpg",
                                          "deadline_ms": 500,
                                          "privacy_level": "NORMAL",
                                          "priority_profile": "BALANCED"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.task_id")
                                .value("TASK-201")
                )
                .andExpect(
                        jsonPath("$.task_type")
                                .value("image_classification")
                )
                .andExpect(
                        jsonPath("$.image_width")
                                .value(800)
                )
                .andExpect(
                        jsonPath("$.image_height")
                                .value(600)
                )
                .andExpect(
                        jsonPath("$.total_pixels")
                                .value(480000)
                )
                .andExpect(
                        jsonPath("$.complexity")
                                .value("MEDIUM")
                )
                .andExpect(
                        jsonPath("$.privacy_level")
                                .value("NORMAL")
                )
                .andExpect(
                        jsonPath("$.priority_profile")
                                .value("BALANCED")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }

    @Test
    void integrationProfileEndpointShouldReturnTaskProfile()
            throws Exception {

        TaskProfileResult result =
                new TaskProfileResult(
                        "TASK-201",
                        "image_classification",
                        "input.jpg",
                        12000,
                        800,
                        600,
                        480000,
                        "MEDIUM",
                        500.0,
                        "NORMAL",
                        "BALANCED",
                        "success"
                );

        when(
                taskProfilerService.profileTask(
                        any(TaskProfileRequest.class)
                )
        ).thenReturn(result);

        mockMvc.perform(
                        post("/api/v1/edge/profile")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "task_id": "TASK-201",
                                          "input_path": "input.jpg",
                                          "deadline_ms": 500,
                                          "privacy_level": "NORMAL",
                                          "priority_profile": "BALANCED"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.task_id")
                                .value("TASK-201")
                )
                .andExpect(
                        jsonPath("$.complexity")
                                .value("MEDIUM")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("success")
                );
    }
}