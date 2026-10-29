
package com.adaptive.decision.service;

import com.adaptive.decision.dto.EstimatorInputs;
import com.adaptive.decision.dto.RouteCostInputs;

import java.util.Map;

public record DecisionSupportingInputs(
    EstimatorInputs estimatorInputs,
    RouteCostInputs routeCostInputs,
    Map<String, Double> communicationCosts,
    Map<String, Double> switchingCosts
) {
    public DecisionSupportingInputs {
        if (estimatorInputs == null
                || routeCostInputs == null
                || communicationCosts == null
                || switchingCosts == null) {
            throw new IllegalArgumentException(
                "All supporting inputs are required"
            );
        }

        communicationCosts = Map.copyOf(communicationCosts);
        switchingCosts = Map.copyOf(switchingCosts);
    }
}
