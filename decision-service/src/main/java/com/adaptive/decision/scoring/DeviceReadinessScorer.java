
package com.adaptive.decision.scoring;

public final class DeviceReadinessScorer {


    private DeviceReadinessScorer() {
    }

    public record ReadinessResult(
        double deviceReadiness,
        double adjustedScore
    ) {}

    public static double calculateReadiness(
            double cpuPercent,
            double ramPercent,
            Double batteryPercent,
            Boolean powerPlugged) {

        validatePercent(cpuPercent, "CPU");
        validatePercent(ramPercent, "RAM");

        double cpuReadiness =
            1.0 - cpuPercent / 100.0;

        double ramReadiness =
            1.0 - ramPercent / 100.0;

        double sum = cpuReadiness + ramReadiness;
        int count = 2;

        // Match Python: battery is considered
        // only when the device is unplugged.
        if (Boolean.FALSE.equals(powerPlugged)
                && batteryPercent != null) {

            validatePercent(
                batteryPercent,
                "Battery"
            );

            double batteryReadiness =
                batteryPercent / 100.0;

            sum += batteryReadiness;
            count++;
        }

        return sum / count;
    }

    public static ReadinessResult adjustScore(
            String route,
            double baseScore,
            double readiness) {

        return adjustScore(route, baseScore, readiness, ScoringConfiguration.defaults().readinessStrength());
    }

    public static ReadinessResult adjustScore(String route, double baseScore, double readiness, double strength) {
        if (!Double.isFinite(strength) || strength < 0 || strength > 1)
            throw new IllegalArgumentException("Readiness strength must be between 0 and 1");
        if (!Double.isFinite(baseScore)
                || baseScore < 0.0
                || baseScore > 1.0) {

            throw new IllegalArgumentException(
                "Base score must be between 0 and 1"
            );
        }

        if ("CLOUD".equals(route)) {
            return new ReadinessResult(
                Double.NaN,
                baseScore
            );
        }

        if (!"EDGE".equals(route)
                && !"HYBRID".equals(route)) {

            throw new IllegalArgumentException(
                "Invalid route: " + route
            );
        }

        if (!Double.isFinite(readiness)
                || readiness < 0.0
                || readiness > 1.0) {

            throw new IllegalArgumentException(
                "Device readiness must be between 0 and 1"
            );
        }

        double adjustedScore =
            (1.0 - strength) * baseScore
            + strength * readiness;

        return new ReadinessResult(
            round4(readiness),
            round4(adjustedScore)
        );
    }

    private static void validatePercent(
            double value,
            String name) {

        if (!Double.isFinite(value)
                || value < 0.0
                || value > 100.0) {

            throw new IllegalArgumentException(
                name + " percentage must be between 0 and 100"
            );
        }
    }

    private static double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
