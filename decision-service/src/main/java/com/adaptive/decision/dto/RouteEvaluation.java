package com.adaptive.decision.dto;

import java.util.List;

public record RouteEvaluation(
    String route,
    boolean feasible,
    Double estimatedLatencyMs,
    Double estimatedEnergyJ,
    Double estimatedCarbonG,
    Double communicationCost,
    Double switchingCost,
    double score,
    List<String> rejectionReasons
) {
}
