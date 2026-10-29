
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionResponse;
import com.adaptive.decision.dto.EstimatorResponse;
import com.adaptive.decision.dto.RouteEstimate;
import com.adaptive.decision.dto.RouteEvaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DecisionResponseAssembler {

    private DecisionResponseAssembler() {
    }

    public static DecisionResponse assemble(
            String taskId,
            RoutingResult routing,
            EstimatorResponse estimates,
            Map<String, Double> communicationCosts,
            Map<String, Double> switchingCosts,
            double decisionTimeMs) {

        if (taskId == null || taskId.isBlank()
                || routing == null
                || routing.evaluations() == null
                || estimates == null
                || estimates.estimates() == null
                || communicationCosts == null
                || switchingCosts == null
                || !Double.isFinite(decisionTimeMs)
                || decisionTimeMs < 0) {

            throw new IllegalArgumentException(
                "Invalid response assembly inputs"
            );
        }

        if (!taskId.equals(estimates.requestId())) {
            throw new IllegalArgumentException(
                "Estimator task ID mismatch"
            );
        }

        List<RouteEvaluation> evaluations =
            new ArrayList<>();

        for (RoutingResult.RouteResult result
                : routing.evaluations()) {

            RouteEstimate estimate =
                estimates.estimates().get(result.route());

            Double latency = null;
            Double energy = null;
            Double carbon = null;
            Double communicationCost = null;
            Double switchingCost = null;

            if (estimate != null) {

                latency = validOrNull(
                    estimate.estimatedLatencyMs()
                );

                energy = validOrNull(
                    estimate.energyEstimateJ()
                );

                carbon = validOrNull(
                    estimate.carbonEstimateG()
                );

                if (result.feasible()
                        && !estimate.estimateAvailable()) {

                    throw new IllegalArgumentException(
                        "Feasible route has unavailable estimate"
                    );
                }
            }

            communicationCost = validOrNull(
                communicationCosts.get(result.route())
            );

            switchingCost = validOrNull(
                switchingCosts.get(result.route())
            );

            if (result.feasible()
                    && (latency == null
                    || energy == null
                    || carbon == null
                    || communicationCost == null
                    || switchingCost == null)) {

                throw new IllegalArgumentException(
                    "Feasible route has missing measurements "
                    + "or costs: " + result.route()
                );
            }

            evaluations.add(
                new RouteEvaluation(
                    result.route(),
                    result.feasible(),
                    latency,
                    energy,
                    carbon,
                    communicationCost,
                    switchingCost,
                    result.score(),
                    result.rejectionReasons()
                )
            );
        }

        return new DecisionResponse(
            taskId,
            routing.status(),
            routing.selectedRoute(),
            decisionTimeMs,
            List.copyOf(evaluations),
            DecisionExplainer.explain(routing)
        );
    }

    private static Double validOrNull(Double value) {
        return value != null
            && Double.isFinite(value)
            && value >= 0
                ? value
                : null;
    }
}
