
package com.adaptive.decision.dto;

import java.util.List;

public record DetailedRouteEvaluation(
    String route,
    boolean feasible,
    List<String> rejectionReasons,
    ScoreBreakdown scoring,
    List<String> explanations
) {
    public DetailedRouteEvaluation(String route, boolean feasible, List<String> rejectionReasons, ScoreBreakdown scoring) {
        this(route, feasible, rejectionReasons, scoring, feasible
            ? List.of("Privacy, network, deadline and estimate checks passed.",
                      "Higher utility is better; score includes readiness for EDGE/HYBRID when known.")
            : rejectionReasons.stream().map(reason -> "Rejected: " + reason).toList());
    }
}
