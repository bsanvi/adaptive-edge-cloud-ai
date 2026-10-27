
package com.adaptive.decision.service;

import com.adaptive.decision.dto.*;
import com.adaptive.decision.scoring.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DetailedDecisionResponseAssembler {

    private static final double EPSILON = 1e-8;

    private DetailedDecisionResponseAssembler() {
    }

    public static DetailedDecisionResponse assemble(
            DecisionResponse decision,
            RoutingResult routing,
            List<RoutingEngine.RouteInput> inputs,
            Double deviceReadiness) {

        if (decision == null
                || routing == null
                || routing.evaluations() == null
                || inputs == null) {

            throw new IllegalArgumentException(
                "Detailed response inputs are required"
            );
        }

        if (!Objects.equals(
                decision.selectedRoute(),
                routing.selectedRoute())
                || !Objects.equals(
                    decision.status(),
                    routing.status())) {

            throw new IllegalArgumentException(
                "Decision and routing results do not match"
            );
        }

        List<DetailedRouteEvaluation> details =
            new ArrayList<>();

        for (RoutingResult.RouteResult routeResult
                : routing.evaluations()) {

            String route = routeResult.route();

            List<RoutingEngine.RouteInput> matches =
                inputs.stream()
                    .filter(input ->
                        input != null
                        && Objects.equals(
                            route, input.route()
                        )
                    )
                    .toList();

            if (matches.size() != 1) {
                throw new IllegalArgumentException(
                    "Expected exactly one input for " + route
                );
            }

            RoutingEngine.RouteInput input =
                matches.get(0);

            if (input.feasible() != routeResult.feasible()) {
                throw new IllegalArgumentException(
                    "Feasibility mismatch for " + route
                );
            }

            ScoreBreakdown breakdown = null;

            if (routeResult.feasible()) {

                WeightedScorer.ScoreResult base =
                    WeightedScorer.score(input.metrics());

                double baseScore = base.score();
                double expectedScore = baseScore;

                Double readiness = null;

                if (deviceReadiness != null) {

                    // Match RoutingEngine's four-decimal
                    // base score rounding.
                    baseScore =
                        Math.round(baseScore * 10000.0)
                        / 10000.0;

                    expectedScore =
                        DeviceReadinessScorer.adjustScore(
                            route,
                            baseScore,
                            deviceReadiness
                        ).adjustedScore();

                    if (!"CLOUD".equals(route)) {
                        readiness =
                            Math.round(
                                deviceReadiness * 10000.0
                            ) / 10000.0;
                    }
                }

                if (Math.abs(
                        expectedScore - routeResult.score()
                    ) > EPSILON) {

                    throw new IllegalArgumentException(
                        "Score mismatch for " + route
                    );
                }

                breakdown = new ScoreBreakdown(
                    baseScore,
                    expectedScore,
                    readiness,
                    base.components(),
                    base.contributions(),
                    DecisionWeights.defaults()
                );
            }

            details.add(
                new DetailedRouteEvaluation(
                    route,
                    routeResult.feasible(),
                    routeResult.rejectionReasons(),
                    breakdown
                )
            );
        }

        return new DetailedDecisionResponse(
            decision,
            List.copyOf(details)
        );
    }
}
