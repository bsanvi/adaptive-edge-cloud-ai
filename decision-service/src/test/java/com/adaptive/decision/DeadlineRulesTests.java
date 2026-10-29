
package com.adaptive.decision;

import com.adaptive.decision.rules.DeadlineRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeadlineRulesTests {

    @Test
    void acceptsExecutionBeforeDeadline() {
        assertTrue(
            DeadlineRules.meetsDeadline(1000.0, 600.0)
        );
    }

    @Test
    void acceptsExecutionExactlyAtDeadline() {
        assertTrue(
            DeadlineRules.meetsDeadline(1000.0, 1000.0)
        );
    }

    @Test
    void rejectsExecutionAfterDeadline() {
        assertFalse(
            DeadlineRules.meetsDeadline(1000.0, 1200.0)
        );
    }

    @Test
    void rejectsMissingEstimate() {
        assertFalse(
            DeadlineRules.meetsDeadline(1000.0, null)
        );
    }

    @Test
    void rejectsNegativeExecutionTime() {
        assertFalse(
            DeadlineRules.meetsDeadline(1000.0, -5.0)
        );
    }

    @Test
    void rejectsInvalidDeadline() {
        assertFalse(
            DeadlineRules.meetsDeadline(0.0, 500.0)
        );

        assertFalse(
            DeadlineRules.meetsDeadline(-100.0, 50.0)
        );
    }

    @Test
    void rejectsNonFiniteValues() {
        assertFalse(
            DeadlineRules.meetsDeadline(
                1000.0,
                Double.NaN
            )
        );

        assertFalse(
            DeadlineRules.meetsDeadline(
                Double.POSITIVE_INFINITY,
                500.0
            )
        );
    }
}
