
package com.team.aecai.orchestrator.dto;

import java.util.List;

public record RouteEvaluation(
    String route,
    boolean feasible,
    double estimatedLatencyMs,
    double estimatedEnergyJ,
    double estimatedCarbonG,
    double communicationCost,
    double switchingCost,
    double score,
    List<String> rejectionReasons
) {
}
