package com.edgecloud.edge.dto;

public record EdgeMetrics(
        double cpu_percent,
        double ram_percent,
        long ram_used_mb,
        long ram_available_mb,
        Double battery_percent,
        Boolean power_plugged,
        String timestamp
) {
}