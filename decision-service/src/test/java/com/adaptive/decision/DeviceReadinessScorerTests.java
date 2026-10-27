
package com.adaptive.decision;

import com.adaptive.decision.scoring.DeviceReadinessScorer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceReadinessScorerTests {

    @Test
    void calculatesReadinessForPluggedDevice() {

        double readiness =
            DeviceReadinessScorer.calculateReadiness(
                30.0,
                50.0,
                80.0,
                true
            );

        assertEquals(0.6, readiness, 1e-9);
    }

    @Test
    void calculatesReadinessForUnpluggedDevice() {

        double readiness =
            DeviceReadinessScorer.calculateReadiness(
                30.0,
                50.0,
                90.0,
                false
            );

        assertEquals(0.7, readiness, 1e-9);
    }

    @Test
    void ignoresMissingBatteryData() {

        double readiness =
            DeviceReadinessScorer.calculateReadiness(
                30.0,
                50.0,
                null,
                false
            );

        assertEquals(0.6, readiness, 1e-9);
    }

    @Test
    void adjustsEdgeScore() {

        var result =
            DeviceReadinessScorer.adjustScore(
                "EDGE",
                0.8,
                0.6
            );

        assertEquals(
            0.78,
            result.adjustedScore(),
            1e-9
        );
    }

    @Test
    void adjustsHybridScore() {

        var result =
            DeviceReadinessScorer.adjustScore(
                "HYBRID",
                0.7,
                0.5
            );

        assertEquals(
            0.68,
            result.adjustedScore(),
            1e-9
        );
    }

    @Test
    void cloudScoreRemainsUnchanged() {

        var result =
            DeviceReadinessScorer.adjustScore(
                "CLOUD",
                0.8,
                0.5
            );

        assertEquals(
            0.8,
            result.adjustedScore(),
            1e-9
        );
    }

    @Test
    void rejectsInvalidCpuPercentage() {

        assertThrows(
            IllegalArgumentException.class,
            () -> DeviceReadinessScorer.calculateReadiness(
                120.0,
                50.0,
                80.0,
                true
            )
        );
    }

    @Test
    void rejectsUnknownRoute() {

        assertThrows(
            IllegalArgumentException.class,
            () -> DeviceReadinessScorer.adjustScore(
                "UNKNOWN",
                0.8,
                0.6
            )
        );
    }
}
