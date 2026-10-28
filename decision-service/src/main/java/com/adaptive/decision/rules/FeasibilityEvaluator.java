
package com.adaptive.decision.rules;

import java.util.ArrayList;
import java.util.List;

public class FeasibilityEvaluator {

    public record Result(
            String route,
            boolean feasible,
            List<String> rejectionReasons
    ) {}

    public static Result evaluate(
            String route,
            String privacyLevel,
            double deadlineMs,
            Double estimatedTimeMs,
            boolean networkConnected,
            double networkLatencyMs,
            double networkBandwidthMbps) {

        return evaluate(route, privacyLevel, deadlineMs, estimatedTimeMs, networkConnected,
            networkLatencyMs, networkBandwidthMbps, com.adaptive.decision.scoring.ScoringConfiguration.defaults());
    }

    public static Result evaluate(String route, String privacyLevel, double deadlineMs,
            Double estimatedTimeMs, boolean networkConnected, double networkLatencyMs,
            double networkBandwidthMbps, com.adaptive.decision.scoring.ScoringConfiguration config) {
        List<String> reasons = new ArrayList<>();

        if (route == null
                || !List.of("EDGE", "CLOUD", "HYBRID")
                    .contains(route.toUpperCase())) {

            return new Result(
                route,
                false,
                List.of("INVALID_ROUTE")
            );
        }

        if (!PrivacyRules.isRouteAllowed(
                privacyLevel, route)) {
            reasons.add("PRIVACY_RESTRICTION");
        }

        if (estimatedTimeMs == null
                || !Double.isFinite(estimatedTimeMs)
                || estimatedTimeMs < 0) {
            reasons.add("MISSING_OR_INVALID_ESTIMATE");

        } else if (!DeadlineRules.meetsDeadline(
                deadlineMs, estimatedTimeMs)) {
            reasons.add("DEADLINE_EXCEEDED");
        }

        if (!NetworkRules.isRouteAllowed(
                route,
                networkConnected,
                networkLatencyMs,
                networkBandwidthMbps, config.maxNetworkLatencyMs(), config.minBandwidthMbps())) {
            reasons.add("NETWORK_RESTRICTION");
        }

        return new Result(
            route.toUpperCase(),
            reasons.isEmpty(),
            List.copyOf(reasons)
        );
    }
}
