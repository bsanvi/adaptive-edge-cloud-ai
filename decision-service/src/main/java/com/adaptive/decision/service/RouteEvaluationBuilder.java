
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;
import com.adaptive.decision.dto.EstimatorResponse;
import com.adaptive.decision.dto.RouteCostInputs;
import com.adaptive.decision.dto.RouteEstimate;
import com.adaptive.decision.rules.FeasibilityEvaluator;
import com.adaptive.decision.scoring.WeightedScorer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RouteEvaluationBuilder {

    private static final List<String> ROUTES =
        List.of("EDGE", "CLOUD", "HYBRID");

    private RouteEvaluationBuilder() {
    }

    public static List<RoutingEngine.RouteInput> build(
            DecisionRequest request,
            EstimatorResponse response,
            RouteCostInputs costs) {

        if (request == null
                || request.task() == null
                || request.network() == null
                || response == null
                || costs == null) {

            throw new IllegalArgumentException(
                "Decision request, estimator response "
                + "and monetary costs are required"
            );
        }

        Map<String, RouteEstimate> estimates =
            response.estimates();

        if (estimates == null) {
            throw new IllegalArgumentException(
                "Estimator response has no estimates map"
            );
        }

        if (response.requestId() == null
                || !response.requestId().equals(
                    request.task().taskId())) {

            throw new IllegalArgumentException(
                "Estimator request ID mismatch"
            );
        }

        var task = request.task();
        var network = request.network();

        List<RoutingEngine.RouteInput> results =
            new ArrayList<>();

        for (String route : ROUTES) {

            RouteEstimate estimate = estimates.get(route);

            boolean available = estimate != null
                && estimate.estimateAvailable();

            Double estimatedTime = available
                ? estimate.estimatedLatencyMs()
                : null;

            var feasibility = FeasibilityEvaluator.evaluate(
                route,
                task.privacyLevel(),
                task.deadlineMs(),
                estimatedTime,
                network.connected(),
                network.latencyMs(),
                network.bandwidthMbps()
            );

            List<String> reasons = new ArrayList<>(
                feasibility.rejectionReasons()
            );

            if (!available) {
                reasons.add("ESTIMATE_UNAVAILABLE");
            }

            if (available && !validNonnegative(
                    estimate.energyEstimateJ())) {
                reasons.add("MISSING_OR_INVALID_ENERGY");
            }

            if (available && !validNonnegative(
                    estimate.carbonEstimateG())) {
                reasons.add("MISSING_OR_INVALID_CARBON");
            }

            boolean feasible = reasons.isEmpty();

            WeightedScorer.Metrics metrics = null;

            if (feasible) {

                Double cost = costs.monetaryCosts().get(route);

                if (!validNonnegative(cost)) {
                    throw new IllegalArgumentException(
                        "Missing or invalid monetary cost for "
                        + route
                    );
                }

                metrics = new WeightedScorer.Metrics(
                    estimate.estimatedLatencyMs(),
                    estimate.energyEstimateJ(),
                    cost,
                    estimate.carbonEstimateG()
                );
            }

            results.add(
                new RoutingEngine.RouteInput(
                    route,
                    feasible,
                    metrics,
                    List.copyOf(reasons)
                )
            );
        }

        return List.copyOf(results);
    }

    private static boolean validNonnegative(
            Double value) {

        return value != null
            && Double.isFinite(value)
            && value >= 0.0;
    }
}
