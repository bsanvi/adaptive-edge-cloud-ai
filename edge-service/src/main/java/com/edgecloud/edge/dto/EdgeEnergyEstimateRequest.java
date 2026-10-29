package com.edgecloud.edge.dto;

public record EdgeEnergyEstimateRequest(
        String task_id,
        double execution_time_ms,
        double cpu_percent_before,
        double cpu_percent_after
) {
}
