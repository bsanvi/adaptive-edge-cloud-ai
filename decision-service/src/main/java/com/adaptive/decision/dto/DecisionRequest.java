package com.adaptive.decision.dto;

public record DecisionRequest(
    TaskProfile task,
    DeviceMetrics device,
    NetworkMetrics network,
    String currentRoute
) {
}