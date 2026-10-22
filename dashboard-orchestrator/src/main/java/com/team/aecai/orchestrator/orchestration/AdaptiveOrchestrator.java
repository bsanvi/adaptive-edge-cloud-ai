
package com.team.aecai.orchestrator.orchestration;

import com.team.aecai.orchestrator.client.*;
import com.team.aecai.orchestrator.dto.*;
import com.team.aecai.orchestrator.network.NetworkMonitor;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class AdaptiveOrchestrator {

    private final EdgeApiClient edge;
    private final DecisionApiClient decisionClient;
    private final CloudApiClient cloud;
    private final NetworkMonitor networkMonitor;

    public AdaptiveOrchestrator(
            EdgeApiClient edge,
            DecisionApiClient decisionClient,
            CloudApiClient cloud,
            NetworkMonitor networkMonitor) {

        this.edge = edge;
        this.decisionClient = decisionClient;
        this.cloud = cloud;
        this.networkMonitor = networkMonitor;
    }

    public OrchestrationResult execute(
            TaskRequest task,
            String currentRoute) {

        long startTime = System.nanoTime();

        // Step 1: Collect task and system information
        TaskProfile profile = edge.profileTask(task);
        DeviceMetrics device = edge.getMetrics();
        NetworkMetrics network =
                networkMonitor.measureNetwork();

        // Step 2: Request a route decision
        DecisionRequest decisionRequest =
                new DecisionRequest(
                        profile,
                        device,
                        network,
                        currentRoute
                );

        DecisionResponse decision =
                decisionClient.evaluate(decisionRequest);

        if (decision == null) {
            throw new IllegalStateException(
                    "Decision service returned no response"
            );
        }

        // Step 3: Handle no feasible route
        if ("NO_FEASIBLE_ROUTE".equals(decision.status())) {
            return result(
                    task.taskId(),
                    "NO_FEASIBLE_ROUTE",
                    null,
                    null,
                    false,
                    null,
                    decision,
                    startTime,
                    "No execution route satisfies the constraints"
            );
        }

        if (!"ROUTE_SELECTED".equals(decision.status())
                || decision.selectedRoute() == null) {
            throw new IllegalStateException(
                    "Invalid decision response"
            );
        }

        String selectedRoute = decision.selectedRoute();
        String actualRoute = selectedRoute;
        boolean fallbackUsed = false;

        InferenceRequest inferenceRequest =
                new InferenceRequest(
                        task.taskId(),
                        task.features()
                );

        InferenceResponse prediction;

        // Step 4: Execute selected route
        switch (selectedRoute) {

            case "EDGE":
                prediction = edge.predict(inferenceRequest);
                break;

            case "CLOUD":
                try {
                    prediction = cloud.predict(inferenceRequest);

                } catch (RestClientException ex) {

                    // Only fallback if Decision Engine
                    // declared EDGE feasible.
                    if (!isEdgeFeasible(decision)) {
                        throw ex;
                    }

                    prediction = edge.predict(inferenceRequest);
                    actualRoute = "EDGE";
                    fallbackUsed = true;
                }
                break;

            case "HYBRID":
                try {
                    HybridPreprocessResponse prepared =
                            edge.hybridPreprocess(
                                    inferenceRequest
                            );

                    HybridCloudRequest cloudRequest =
                            new HybridCloudRequest(
                                    task.taskId(),
                                    prepared.processedFeatures()
                            );

                    HybridInferenceResult cloudResult =
                            cloud.hybridInfer(cloudRequest);

                    prediction =
                            edge.hybridPostprocess(cloudResult);

                } catch (RestClientException ex) {

                    if (!isEdgeFeasible(decision)) {
                        throw ex;
                    }

                    prediction = edge.predict(inferenceRequest);
                    actualRoute = "EDGE";
                    fallbackUsed = true;
                }
                break;

            default:
                throw new IllegalStateException(
                        "Unknown execution route: " + selectedRoute
                );
        }

        if (prediction == null) {
            throw new IllegalStateException(
                    "Execution returned no prediction"
            );
        }

        // Step 5: Return execution results
        return result(
                task.taskId(),
                "SUCCESS",
                selectedRoute,
                actualRoute,
                fallbackUsed,
                prediction,
                decision,
                startTime,
                fallbackUsed
                    ? "Cloud failed; executed on Edge"
                    : "Task executed successfully"
        );
    }

    private boolean isEdgeFeasible(
            DecisionResponse decision) {

        if (decision.evaluations() == null) {
            return false;
        }

        return decision.evaluations().stream()
                .anyMatch(evaluation ->
                        "EDGE".equals(evaluation.route())
                        && evaluation.feasible()
                );
    }

    private OrchestrationResult result(
            String taskId,
            String status,
            String selectedRoute,
            String actualRoute,
            boolean fallbackUsed,
            InferenceResponse prediction,
            DecisionResponse decision,
            long startTime,
            String message) {

        double totalTimeMs =
                (System.nanoTime() - startTime)
                / 1_000_000.0;

        return new OrchestrationResult(
                taskId,
                status,
                selectedRoute,
                actualRoute,
                fallbackUsed,
                prediction,
                decision,
                totalTimeMs,
                message
        );
    }
}
