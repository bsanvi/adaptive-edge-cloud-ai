
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
                networkBandwidthMbps)) {
            reasons.add("NETWORK_RESTRICTION");
        }

        return new Result(
            route.toUpperCase(),
            reasons.isEmpty(),
            List.copyOf(reasons)
        );
    }
}
