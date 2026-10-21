package com.edgecloud.edge.controller;

import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.dto.EdgeTaskRequest;
import com.edgecloud.edge.dto.EdgeTaskResponse;
import com.edgecloud.edge.service.EdgeTaskService;
import com.edgecloud.edge.service.SystemMonitorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EdgeController {

    private final SystemMonitorService systemMonitorService;
    private final EdgeTaskService edgeTaskService;

    public EdgeController(
            SystemMonitorService systemMonitorService,
            EdgeTaskService edgeTaskService
    ) {
        this.systemMonitorService = systemMonitorService;
        this.edgeTaskService = edgeTaskService;
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
                "status", "ok",
                "component", "edge"
        );
    }

    @GetMapping("/edge/metrics")
    public EdgeMetrics edgeMetrics() {
        return systemMonitorService.getEdgeMetrics();
    }

    @PostMapping("/edge/execute")
    public EdgeTaskResponse executeEdgeTask(
            @RequestBody EdgeTaskRequest request
    ) {
        return edgeTaskService.runEdgeTask(request);
    }
}