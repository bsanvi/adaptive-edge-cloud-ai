package com.edgecloud.edge.dto;

public record EdgeTaskResponse(
        String task_id,
        String execution_location,
        String status,
        String task_type,
        EdgeTaskRequest task_input,
        Double execution_time_ms,
        EdgeMetrics metrics_before,
        EdgeMetrics metrics_after,
        ImageResizeResult result,
        String error
) {
}