
package com.adaptive.decision.service;

import com.adaptive.decision.selection.RouteSelector;
import com.adaptive.decision.selection.SwitchingPolicy;
import com.adaptive.decision.scoring.WeightedScorer;
import com.adaptive.decision.scoring.DeviceReadinessScorer;

import java.util.ArrayList;
import java.util.List;

public final class RoutingEngine {

    private RoutingEngine() {
    }

    public record RouteInput(
        String route,
        boolean feasible,
        WeightedScorer.Metrics metrics,
        List<String> rejectionReasons
    ) {}

    // Original method retained for existing callers.
    public static RoutingResult evaluate(
            List<RouteInput> inputs,
            String previousRoute) {

        return evaluate(
            inputs,
            previousRoute,
            null
        );
    }

    // New method includes device readiness.
    public static RoutingResult evaluate(
            List<RouteInput> inputs,
            String previousRoute,
            Double deviceReadiness) {

        if (inputs == null) {
            throw new IllegalArgumentException(
                "Route inputs cannot be null"
            );
        }

        List<RouteSelector.Candidate> candidates =
            new ArrayList<>();

        List<RoutingResult.RouteResult> evaluations =
            new ArrayList<>();

        for (RouteInput input : inputs) {

            if (input == null
                    || input.rejectionReasons() == null) {

                throw new IllegalArgumentException(
                    "Invalid route input"
                );
            }

            double score = 0.0;

            if (input.feasible()) {

                double baseScore =
                    WeightedScorer.score(
                        input.metrics()
                    ).score();

                if (deviceReadiness != null) {

                    // Match Python's four-decimal
                    // base-score rounding.
                    baseScore =
                        Math.round(baseScore * 10000.0)
                        / 10000.0;

                    // EDGE and HYBRID receive the
                    // readiness adjustment.
                    // CLOUD retains its base score.
                    score = DeviceReadinessScorer
                        .adjustScore(
                            input.route(),
                            baseScore,
                            deviceReadiness
                        )
                        .adjustedScore();

                } else {
                    // Preserve old behavior.
                    score = baseScore;
                }
            }

            candidates.add(
                new RouteSelector.Candidate(
                    input.route(),
                    input.feasible(),
                    score
                )
            );

            evaluations.add(
                new RoutingResult.RouteResult(
                    input.route(),
                    input.feasible(),
                    score,
                    List.copyOf(
                        input.rejectionReasons()
                    )
                )
            );
        }

        String selectedRoute =
            SwitchingPolicy.selectWithHysteresis(
                candidates,
                previousRoute
            );

        String status = selectedRoute == null
            ? "NO_FEASIBLE_ROUTE"
            : "ROUTE_SELECTED";

        return new RoutingResult(
            status,
            selectedRoute,
            List.copyOf(evaluations)
        );
    }
}
