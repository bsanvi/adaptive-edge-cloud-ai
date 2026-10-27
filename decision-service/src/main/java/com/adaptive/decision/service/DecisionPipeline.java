
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;
import com.adaptive.decision.dto.EstimatorInputs;
import com.adaptive.decision.dto.EstimatorResponse;
import com.adaptive.decision.dto.RouteCostInputs;

import org.springframework.stereotype.Service;

@Service
public class DecisionPipeline {

    private final EstimationGateway estimationGateway;

    public DecisionPipeline(
            EstimationGateway estimationGateway) {
        this.estimationGateway = estimationGateway;
    }

    // Keep the original method for existing callers.
    public RoutingResult evaluate(
            DecisionRequest request,
            EstimatorInputs estimatorInputs,
            RouteCostInputs costInputs) {

        return evaluateDetailed(
            request,
            estimatorInputs,
            costInputs
        ).routing();
    }

    // New method: preserve the estimator response too.
    public DecisionPipelineResult evaluateDetailed(
            DecisionRequest request,
            EstimatorInputs estimatorInputs,
            RouteCostInputs costInputs) {

        if (request == null
                || estimatorInputs == null
                || costInputs == null) {

            throw new IllegalArgumentException(
                "Decision request and supporting inputs "
                + "must not be null"
            );
        }

        // 1. Obtain estimates from Person 3.
        EstimatorResponse estimates =
            estimationGateway.estimate(
                request,
                estimatorInputs
            );

        // 2. Check feasibility and build scoring inputs.
        var routeInputs =
            RouteEvaluationBuilder.build(
                request,
                estimates,
                costInputs
            );

        // 3. Score, select and apply hysteresis.
        RoutingResult routing =
            RoutingEngine.evaluate(
                routeInputs,
                request.currentRoute()
            );

        // 4. Preserve both results.
        return new DecisionPipelineResult(
            routing,
            estimates
        );
    }
}
