
package com.adaptive.decision.service;

import com.adaptive.decision.dto.EstimatorResponse;

public record DecisionPipelineResult(
    RoutingResult routing,
    EstimatorResponse estimates
) {
    public DecisionPipelineResult {
        if (routing == null || estimates == null) {
            throw new IllegalArgumentException(
                "Routing result and estimator response are required"
            );
        }
    }
}
