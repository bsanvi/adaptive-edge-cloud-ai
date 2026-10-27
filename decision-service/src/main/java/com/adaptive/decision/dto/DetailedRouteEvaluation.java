
package com.adaptive.decision.dto;

import java.util.List;

public record DetailedRouteEvaluation(
    String route,
    boolean feasible,
    List<String> rejectionReasons,
    ScoreBreakdown scoring
) {
}
