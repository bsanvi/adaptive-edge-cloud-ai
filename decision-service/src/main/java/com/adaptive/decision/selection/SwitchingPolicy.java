
package com.adaptive.decision.selection;

import java.util.List;
import java.util.Set;

public final class SwitchingPolicy {

    private static final double THRESHOLD = 0.05;
    private static final double EPSILON = 1e-9;
    
    private static final Set<String> ROUTES =
        Set.of("EDGE", "CLOUD", "HYBRID");

    private SwitchingPolicy() {
    }

    public static String selectWithHysteresis(
            List<RouteSelector.Candidate> candidates,
            String previousRoute) {

        // First, choose the highest-scoring feasible route.
        String bestRoute =
            RouteSelector.selectBest(candidates);

        // Nothing is feasible.
        if (bestRoute == null) {
            return null;
        }

        // First decision: no previous route exists.
        if (previousRoute == null
                || previousRoute.isBlank()) {
            return bestRoute;
        }

        // Reject unknown previous routes.
        if (!ROUTES.contains(previousRoute)) {
            throw new IllegalArgumentException(
                "Invalid previous route: " + previousRoute
            );
        }

        // Find the best route's score.
        double bestScore = candidates.stream()
            .filter(c ->
                c.feasible()
                && c.route().equals(bestRoute)
            )
            .mapToDouble(RouteSelector.Candidate::score)
            .max()
            .orElseThrow();

        // Find the previous route, but only if feasible.
        var previousCandidate = candidates.stream()
            .filter(c ->
                c.feasible()
                && c.route().equals(previousRoute)
            )
            .findFirst();

        // Previous route is unavailable or infeasible.
        if (previousCandidate.isEmpty()) {
            return bestRoute;
        }

        double previousScore =
            previousCandidate.get().score();

        // Keep the old route for small improvements.
        if (bestScore - previousScore <= THRESHOLD + EPSILON) {
           return previousRoute;
}

        // Switch only for a meaningful improvement.
        return bestRoute;
    }
}
