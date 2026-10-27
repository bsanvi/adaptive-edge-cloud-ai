
package com.adaptive.decision.scoring;

public final class Normalizer {

    private Normalizer() {
        // Utility class: no objects required.
    }

    public static double normalize(
            double value,
            double min,
            double max) {

        if (!Double.isFinite(value)
                || !Double.isFinite(min)
                || !Double.isFinite(max)) {
            throw new IllegalArgumentException(
                "Normalization inputs must be finite"
            );
        }

        if (max <= min) {
            throw new IllegalArgumentException(
                "Maximum must be greater than minimum"
            );
        }

        double result = (value - min) / (max - min);

        // Clamp the value between 0 and 1.
        return Math.max(0.0, Math.min(1.0, result));
    }

    public static double lowerIsBetter(
            double value,
            double min,
            double max) {

        return 1.0 - normalize(value, min, max);
    }

    public static double higherIsBetter(
            double value,
            double min,
            double max) {

        return normalize(value, min, max);
    }
}
