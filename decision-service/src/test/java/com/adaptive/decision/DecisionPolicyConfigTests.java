
package com.adaptive.decision;

import com.adaptive.decision.service.DecisionPolicyConfig;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecisionPolicyConfigTests {

    private Map<String, Double> costs() {
        return Map.of(
            "EDGE", 0.1,
            "CLOUD", 0.2,
            "HYBRID", 0.15
        );
    }

    @Test
    void acceptsValidConfiguration() {

        var config = new DecisionPolicyConfig(
            256L,
            128L,
            500.0,
            400.0,
            costs(),
            costs()
        );

        assertEquals(
            256L,
            config.estimatedFeatureBytes()
        );

        assertEquals(
            3,
            config.monetaryCosts().size()
        );
    }

    @Test
    void rejectsInvalidFeatureSize() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new DecisionPolicyConfig(
                0L,
                128L,
                500.0,
                400.0,
                costs(),
                costs()
            )
        );
    }

    @Test
    void rejectsInvalidCarbonIntensity() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new DecisionPolicyConfig(
                256L,
                128L,
                Double.NaN,
                400.0,
                costs(),
                costs()
            )
        );
    }

    @Test
    void rejectsMissingRouteCost() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new DecisionPolicyConfig(
                256L,
                128L,
                500.0,
                400.0,
                Map.of("EDGE", 0.1),
                costs()
            )
        );
    }

    @Test
    void rejectsNegativeSwitchingCost() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new DecisionPolicyConfig(
                256L,
                128L,
                500.0,
                400.0,
                costs(),
                Map.of(
                    "EDGE", 0.0,
                    "CLOUD", -1.0,
                    "HYBRID", 0.0
                )
            )
        );
    }
}
