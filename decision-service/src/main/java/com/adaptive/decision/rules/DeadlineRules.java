
package com.adaptive.decision.rules;

public class DeadlineRules {

    public static boolean meetsDeadline(
            double deadlineMs,
            Double estimatedTimeMs) {

        // Invalid or missing estimates are rejected.
        if (estimatedTimeMs == null) {
            return false;
        }

        if (!Double.isFinite(estimatedTimeMs)
                || estimatedTimeMs < 0) {
            return false;
        }

        // A deadline must be positive and finite.
        if (!Double.isFinite(deadlineMs)
                || deadlineMs <= 0) {
            return false;
        }

        // Execution must finish before or
        // exactly at the deadline.
        return estimatedTimeMs <= deadlineMs;
    }
}
