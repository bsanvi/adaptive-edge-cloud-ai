
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;
import com.adaptive.decision.dto.EstimatorInputs;
import com.adaptive.decision.dto.EstimatorResponse;
import com.adaptive.decision.dto.RouteCostInputs;

import com.adaptive.decision.scoring.DeviceReadinessScorer;

import org.springframework.stereotype.Service;

@Service
public class DecisionPipeline {

    private final EstimationGateway estimationGateway;

    public DecisionPipeline(
            EstimationGateway estimationGateway) {

        this.estimationGateway = estimationGateway;
    }

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

        if (request.device() == null) {
            throw new IllegalArgumentException(
                "Device metrics are required"
            );
        }

        // Step 1: Obtain estimates from Person 3.
        EstimatorResponse estimates =
            estimationGateway.estimate(
                request,
                estimatorInputs
            );

        // Step 2: Apply feasibility rules.
        var routeInputs =
            RouteEvaluationBuilder.build(
                request,
                estimates,
                costInputs
            );

        // Step 3: Calculate local device readiness.
        //
        // Person 4 does not supply powerPlugged.
        // Therefore battery is excluded until
        // its charging status can be verified.

        double readiness =
            DeviceReadinessScorer.calculateReadiness(
                request.device().cpuPercent(),
                request.device().ramPercent(),
                null,
                null
            );

        // Step 4: Score routes and apply hysteresis.
        RoutingResult routing =
            RoutingEngine.evaluate(
                routeInputs,
                request.currentRoute(),
                readiness
            );

        // Step 5: Return routing and estimator data.
        return new DecisionPipelineResult(
            routing,
            estimates
        );
    }
}
