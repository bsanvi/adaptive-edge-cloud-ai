
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

    private final com.adaptive.decision.scoring.ScoringConfiguration config;

    public DecisionPipeline(EstimationGateway gateway) {
        this(gateway, com.adaptive.decision.scoring.ScoringConfiguration.defaults());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DecisionPipeline(
            EstimationGateway estimationGateway,
            com.adaptive.decision.scoring.ScoringConfiguration config) {

        this.estimationGateway = estimationGateway;
        this.config = config;
    }

    public void validateRequest(DecisionRequest request) {
        DecisionRequestValidator.validate(request, config);
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

        if (request.task() == null || request.network() == null || request.device() == null) {
            throw new IllegalArgumentException(
                "Device metrics are required"
            );
        }

        validateRequest(request);

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
                costInputs, config
            );

        // Defend hard capability constraints even if an upstream response is inconsistent.
        routeInputs = routeInputs.stream().map(input -> {
            boolean capable = switch (input.route()) {
                case "EDGE" -> estimatorInputs.edgeCapable();
                case "CLOUD" -> estimatorInputs.cloudCapable();
                case "HYBRID" -> estimatorInputs.edgeCapable() && estimatorInputs.cloudCapable();
                default -> false;
            };
            if (capable) return input;
            var reasons = new java.util.ArrayList<>(input.rejectionReasons());
            reasons.add("EXECUTION_CAPABILITY_UNAVAILABLE");
            return new RoutingEngine.RouteInput(input.route(), false, null, java.util.List.copyOf(reasons));
        }).toList();

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
                readiness, config, request.task().priorityProfile()
            );

        // Step 5: Return routing and estimator data.
        return new DecisionPipelineResult(
            routing,
            estimates,
            routeInputs,
            readiness, config, request.task().priorityProfile()
        );
    }
}
