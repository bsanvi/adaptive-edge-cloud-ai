package com.edgecloud.edge.controller;

import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.dto.EdgeTaskRequest;
import com.edgecloud.edge.dto.EdgeTaskResponse;
import com.edgecloud.edge.dto.HybridPostprocessRequest;
import com.edgecloud.edge.dto.HybridPostprocessResult;
import com.edgecloud.edge.dto.HybridPreprocessRequest;
import com.edgecloud.edge.dto.HybridPreprocessResult;
import com.edgecloud.edge.dto.MlInferenceRequest;
import com.edgecloud.edge.dto.MlInferenceResult;
import com.edgecloud.edge.dto.TaskProfileRequest;
import com.edgecloud.edge.dto.TaskProfileResult;

import com.edgecloud.edge.service.EdgeTaskService;
import com.edgecloud.edge.service.HybridPostprocessService;
import com.edgecloud.edge.service.HybridPreprocessService;
import com.edgecloud.edge.service.MlInferenceService;
import com.edgecloud.edge.service.SystemMonitorService;
import com.edgecloud.edge.service.TaskProfilerService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EdgeController {

    private final SystemMonitorService systemMonitorService;
    private final EdgeTaskService edgeTaskService;
    private final MlInferenceService mlInferenceService;
    private final HybridPreprocessService hybridPreprocessService;
    private final HybridPostprocessService hybridPostprocessService;
    private final TaskProfilerService taskProfilerService;

    public EdgeController(
            SystemMonitorService systemMonitorService,
            EdgeTaskService edgeTaskService,
            MlInferenceService mlInferenceService,
            HybridPreprocessService hybridPreprocessService,
            HybridPostprocessService hybridPostprocessService,
            TaskProfilerService taskProfilerService
    ) {
        this.systemMonitorService = systemMonitorService;
        this.edgeTaskService = edgeTaskService;
        this.mlInferenceService = mlInferenceService;
        this.hybridPreprocessService = hybridPreprocessService;
        this.hybridPostprocessService = hybridPostprocessService;
        this.taskProfilerService = taskProfilerService;
    }

    @GetMapping({
            "/health",
            "/api/v1/edge/health"
    })
    public Map<String, String> healthCheck() {
        return Map.of(
                "status", "ok",
                "component", "edge"
        );
    }

    @GetMapping({
            "/edge/metrics",
            "/api/v1/edge/metrics"
    })
    public EdgeMetrics edgeMetrics() {
        return systemMonitorService.getEdgeMetrics();
    }

    @PostMapping("/edge/execute")
    public EdgeTaskResponse executeEdgeTask(
            @RequestBody EdgeTaskRequest request
    ) {
        return edgeTaskService.runEdgeTask(request);
    }

    @PostMapping({
            "/edge/classify",
            "/api/v1/edge/predict"
    })
    public MlInferenceResult classifyImage(
            @RequestBody MlInferenceRequest request
    ) {
        return mlInferenceService.classifyImage(
                request.input_path()
        );
    }

    @PostMapping({
            "/edge/hybrid/preprocess",
            "/api/v1/edge/hybrid/preprocess"
    })
    public HybridPreprocessResult preprocessHybridTask(
            @RequestBody HybridPreprocessRequest request
    ) {
        return hybridPreprocessService.preprocess(request);
    }

    @PostMapping({
            "/edge/hybrid/postprocess",
            "/api/v1/edge/hybrid/postprocess"
    })
    public HybridPostprocessResult postprocessHybridTask(
            @RequestBody HybridPostprocessRequest request
    ) {
        return hybridPostprocessService.postprocess(request);
    }

    @PostMapping({
            "/edge/profile",
            "/api/v1/edge/profile"
    })
    public TaskProfileResult profileTask(
            @RequestBody TaskProfileRequest request
    ) {
        return taskProfilerService.profileTask(request);
    }
}
