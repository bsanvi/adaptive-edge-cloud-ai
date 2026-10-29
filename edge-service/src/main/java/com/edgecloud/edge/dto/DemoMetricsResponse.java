package com.edgecloud.edge.dto;

public record DemoMetricsResponse(
        boolean active,
        String measurement_type,
        EdgeMetrics metrics
) {
}
