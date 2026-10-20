
package com.team.aecai.orchestrator.dto;

import java.util.List;

public record DecisionResponse(
    String taskId,
    String status,
    String selectedRoute,
    double decisionTimeMs,
    List<RouteEvaluation> evaluations,
    List<String> reasons
) {
}
