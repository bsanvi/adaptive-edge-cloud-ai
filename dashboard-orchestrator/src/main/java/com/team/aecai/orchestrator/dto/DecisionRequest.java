
package com.team.aecai.orchestrator.dto;

public record DecisionRequest(
    TaskProfile task,
    DeviceMetrics device,
    NetworkMetrics network,
    String currentRoute
) {
}
