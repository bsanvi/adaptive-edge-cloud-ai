
package com.adaptive.decision.service;

import com.adaptive.decision.dto.EstimatorResponse;

public record DecisionPipelineResult(
    RoutingResult routing,
    EstimatorResponse estimates,
    java.util.List<RoutingEngine.RouteInput> routeInputs,
    Double deviceReadiness,
    com.adaptive.decision.scoring.ScoringConfiguration scoringConfiguration,
    String priorityProfile
) {
    public DecisionPipelineResult(RoutingResult routing, EstimatorResponse estimates) {
        this(routing, estimates, java.util.List.of(), null, com.adaptive.decision.scoring.ScoringConfiguration.defaults(), "BALANCED");
    }

    public DecisionPipelineResult {
        routeInputs = java.util.List.copyOf(routeInputs);
        if (routing == null || estimates == null) {
            throw new IllegalArgumentException(
                "Routing result and estimator response are required"
            );
        }
    }
}
