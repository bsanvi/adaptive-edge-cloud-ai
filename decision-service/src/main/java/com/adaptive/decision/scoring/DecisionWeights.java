
package com.adaptive.decision.scoring;

import java.util.Map;

public final class DecisionWeights {

    private DecisionWeights() {
    }

    public static Map<String, Double> defaults() {

        return ScoringConfiguration.defaults().weights("BALANCED");
    }
}
