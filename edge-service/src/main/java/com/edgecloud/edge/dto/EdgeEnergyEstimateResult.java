package com.edgecloud.edge.dto;

public record EdgeEnergyEstimateResult(
        String task_id,
        double average_cpu_percent,
        double execution_time_ms,
        double estimated_power_watts,
        double estimated_energy_joules,
        String measurement_type,
        String status
) {
}
