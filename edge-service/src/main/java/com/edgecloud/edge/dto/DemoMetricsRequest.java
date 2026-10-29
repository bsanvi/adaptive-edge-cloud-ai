package com.edgecloud.edge.dto;

public record DemoMetricsRequest(
        double cpu_percent,
        double ram_percent,
        long ram_used_mb,
        long ram_available_mb,
        double battery_percent,
        boolean power_plugged
) {
}
