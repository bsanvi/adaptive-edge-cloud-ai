
package com.adaptive.decision.service;

import java.util.Map;
import java.util.Set;

public record DecisionPolicyConfig(
    long estimatedFeatureBytes,
    long estimatedResultBytes,
    double edgeCarbonIntensityGPerKwh,
    double cloudCarbonIntensityGPerKwh,
    Map<String, Double> monetaryCosts,
    Map<String, Double> switchingCosts
) {

    private static final Set<String> ROUTES =
        Set.of("EDGE", "CLOUD", "HYBRID");

    public DecisionPolicyConfig {

        if (estimatedFeatureBytes < 1
                || estimatedFeatureBytes > 1_048_576
                || estimatedResultBytes < 1
                || estimatedResultBytes > 1_048_576) {
            throw new IllegalArgumentException(
                "Feature and result sizes must be "
                + "between 1 and 1048576 bytes"
            );
        }

        if (!valid(edgeCarbonIntensityGPerKwh)
                || !valid(cloudCarbonIntensityGPerKwh)) {
            throw new IllegalArgumentException(
                "Carbon intensities must be finite and nonnegative"
            );
        }

        monetaryCosts = validateCosts(
            monetaryCosts, "monetary"
        );

        switchingCosts = validateCosts(
            switchingCosts, "switching"
        );
    }

    private static Map<String, Double> validateCosts(
            Map<String, Double> costs,
            String label) {

        if (costs == null
                || !costs.keySet().containsAll(ROUTES)) {
            throw new IllegalArgumentException(
                "Missing " + label + " cost for a route"
            );
        }

        for (String route : ROUTES) {
            if (!valid(costs.get(route))) {
                throw new IllegalArgumentException(
                    "Invalid " + label + " cost for " + route
                );
            }
        }

        return Map.copyOf(costs);
    }

    private static boolean valid(Double value) {
        return value != null
            && Double.isFinite(value)
            && value >= 0.0;
    }
}
