
package com.adaptive.decision.dto;

import java.util.Map;

public record RouteCostInputs(
    Map<String, Double> monetaryCosts
) {
    public RouteCostInputs {
        if (monetaryCosts == null) {
            throw new IllegalArgumentException(
                "Monetary costs are required"
            );
        }
        monetaryCosts = Map.copyOf(monetaryCosts);
    }
}
