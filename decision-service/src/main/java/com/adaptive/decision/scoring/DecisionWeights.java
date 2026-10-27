
package com.adaptive.decision.scoring;

import java.util.Map;

public final class DecisionWeights {

    private DecisionWeights() {
    }

    public static Map<String, Double> defaults() {

        return Map.of(
            "time", 0.50,
            "energy", 0.20,
            "cost", 0.20,
            "carbon", 0.10
        );
    }
}
