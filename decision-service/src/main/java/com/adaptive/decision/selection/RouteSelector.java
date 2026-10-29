package com.adaptive.decision.selection;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class RouteSelector {

    private static final Set<String> ROUTES =
        Set.of("EDGE", "CLOUD", "HYBRID");

    private RouteSelector() {
    }

    public record Candidate(
        String route,
        boolean feasible,
        double score
    ) {}

    public static String selectBest(
            List<Candidate> candidates) {

        if (candidates == null) {
            throw new IllegalArgumentException(
                "Candidate list cannot be null"
            );
        }

        for (Candidate candidate : candidates) {
            if (candidate == null
                    || !ROUTES.contains(candidate.route())
                    || !Double.isFinite(candidate.score())) {
                throw new IllegalArgumentException(
                    "Invalid route candidate"
                );
            }
        }

        return candidates.stream()
            .filter(Candidate::feasible)
            .max(
                Comparator.comparingDouble(Candidate::score)
                    .thenComparing(
                        Candidate::route,
                        Comparator.reverseOrder()
                    )
            )
            .map(Candidate::route)
            .orElse(null);
    }
}
