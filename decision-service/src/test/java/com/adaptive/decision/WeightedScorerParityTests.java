
package com.adaptive.decision;

import com.adaptive.decision.scoring.WeightedScorer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeightedScorerParityTests {

    @Test
    void matchesPythonConfiguredNormalization() {

        var metrics = new WeightedScorer.Metrics(
            2500.0,
            50.0,
            0.5,
            0.05
        );

        var result = WeightedScorer.score(metrics);

        assertEquals(0.5, result.score(), 1e-9);

        assertEquals(
            0.5,
            result.components().get("time"),
            1e-9
        );

        assertEquals(
            0.5,
            result.components().get("energy"),
            1e-9
        );

        assertEquals(
            0.5,
            result.components().get("cost"),
            1e-9
        );

        assertEquals(
            0.5,
            result.components().get("carbon"),
            1e-9
        );
    }

    @Test
    void zeroUsageProducesMaximumScore() {

        var metrics = new WeightedScorer.Metrics(
            0.0, 0.0, 0.0, 0.0
        );

        var result = WeightedScorer.score(metrics);

        assertEquals(1.0, result.score(), 1e-9);
    }

    @Test
    void maximumConfiguredUsageProducesZeroScore() {

        var metrics = new WeightedScorer.Metrics(
            5000.0,
            100.0,
            1.0,
            0.1
        );

        var result = WeightedScorer.score(metrics);

        assertEquals(0.0, result.score(), 1e-9);
    }
}
