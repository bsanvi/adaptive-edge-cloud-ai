
package com.adaptive.decision.dto;

import java.util.List;

public record DetailedDecisionResponse(
    DecisionResponse decision,
    List<DetailedRouteEvaluation> details
) {
}
