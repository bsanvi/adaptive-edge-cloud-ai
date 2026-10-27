
package com.adaptive.decision;

import com.adaptive.decision.rules.FeasibilityEvaluator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeasibilityEvaluatorTests {

    @Test
    void acceptsFeasibleEdgeRoute() {
        var result = FeasibilityEvaluator.evaluate(
            "EDGE", "PRIVATE",
            1000.0, 600.0,
            false, 0.0, 0.0
        );

        assertTrue(result.feasible());
        assertTrue(result.rejectionReasons().isEmpty());
    }

    @Test
    void rejectsCloudForPrivateTask() {
        var result = FeasibilityEvaluator.evaluate(
            "CLOUD", "PRIVATE",
            1000.0, 500.0,
            true, 20.0, 50.0
        );

        assertFalse(result.feasible());
        assertTrue(
            result.rejectionReasons()
                .contains("PRIVACY_RESTRICTION")
        );
    }

    @Test
    void rejectsRouteExceedingDeadline() {
        var result = FeasibilityEvaluator.evaluate(
            "EDGE", "PUBLIC",
            1000.0, 1500.0,
            true, 20.0, 50.0
        );

        assertFalse(result.feasible());
        assertTrue(
            result.rejectionReasons()
                .contains("DEADLINE_EXCEEDED")
        );
    }

    @Test
    void rejectsCloudOnSlowNetwork() {
        var result = FeasibilityEvaluator.evaluate(
            "CLOUD", "PUBLIC",
            1000.0, 500.0,
            true, 350.0, 2.0
        );

        assertFalse(result.feasible());
        assertTrue(
            result.rejectionReasons()
                .contains("NETWORK_RESTRICTION")
        );
    }

    @Test
    void collectsMultipleRejectionReasons() {
        var result = FeasibilityEvaluator.evaluate(
            "CLOUD", "PRIVATE",
            1000.0, 1500.0,
            true, 350.0, 2.0
        );

        assertFalse(result.feasible());

        assertTrue(result.rejectionReasons()
            .contains("PRIVACY_RESTRICTION"));

        assertTrue(result.rejectionReasons()
            .contains("DEADLINE_EXCEEDED"));

        assertTrue(result.rejectionReasons()
            .contains("NETWORK_RESTRICTION"));
    }

    @Test
    void rejectsMissingEstimate() {
        var result = FeasibilityEvaluator.evaluate(
            "EDGE", "PUBLIC",
            1000.0, null,
            true, 20.0, 50.0
        );

        assertFalse(result.feasible());
        assertTrue(result.rejectionReasons()
            .contains("MISSING_OR_INVALID_ESTIMATE"));
    }

    @Test
    void rejectsUnknownRoute() {
        var result = FeasibilityEvaluator.evaluate(
            "UNKNOWN", "PUBLIC",
            1000.0, 500.0,
            true, 20.0, 50.0
        );

        assertFalse(result.feasible());
        assertTrue(result.rejectionReasons()
            .contains("INVALID_ROUTE"));
    }
}
