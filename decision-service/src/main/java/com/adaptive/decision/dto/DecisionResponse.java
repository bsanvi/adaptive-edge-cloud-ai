package com.adaptive.decision.dto;

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