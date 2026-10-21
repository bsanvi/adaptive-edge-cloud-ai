package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.dto.EdgeTaskRequest;
import com.edgecloud.edge.dto.EdgeTaskResponse;
import com.edgecloud.edge.dto.ImageResizeResult;

import org.springframework.stereotype.Service;

@Service
public class EdgeTaskService {

    private final SystemMonitorService systemMonitorService;
    private final EdgeExecutorService edgeExecutorService;

    public EdgeTaskService(
            SystemMonitorService systemMonitorService,
            EdgeExecutorService edgeExecutorService
    ) {
        this.systemMonitorService = systemMonitorService;
        this.edgeExecutorService = edgeExecutorService;
    }

    public EdgeTaskResponse runEdgeTask(EdgeTaskRequest request) {

        EdgeMetrics metricsBefore =
                systemMonitorService.getEdgeMetrics();

        String status;
        Double executionTimeMs;
        ImageResizeResult result;
        String error;

        try {

            result = edgeExecutorService.resizeImage(
                    request.input_path(),
                    request.output_path(),
                    request.width(),
                    request.height()
            );

            status = "success";
            executionTimeMs = result.execution_time_ms();
            error = null;

        } catch (Exception exception) {

            status = "failed";
            executionTimeMs = null;
            result = null;
            error = exception.getMessage();
        }

        EdgeMetrics metricsAfter =
                systemMonitorService.getEdgeMetrics();

        return new EdgeTaskResponse(
                request.task_id(),
                "edge",
                status,
                "image_resize",
                request,
                executionTimeMs,
                metricsBefore,
                metricsAfter,
                result,
                error
        );
    }
}