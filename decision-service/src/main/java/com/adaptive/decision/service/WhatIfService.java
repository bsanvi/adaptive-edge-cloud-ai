package com.adaptive.decision.service;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.scoring.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class WhatIfService {
    private final ScoringConfiguration config;
    public WhatIfService(ScoringConfiguration config) { this.config = config; }
    public record Evaluation(String name, String source, WhatIfRequest.Scenario simulatedInputs,
                             DetailedDecisionResponse result) {}
    public record InputChange(String field, Object baseline, Object scenario) {}
    public record Comparison(String name, String baselineRoute, String scenarioRoute,
                             boolean routeChanged, List<String> explanations, List<InputChange> changes) {}
    public record Response(String source, boolean tasksExecuted, String estimatePolicy,
                           Evaluation baseline, List<Evaluation> scenarios, List<Comparison> comparisons) {}
    public Response evaluate(WhatIfRequest input) {
        if (input == null || input.baseline() == null || input.scenarios() == null
                || input.scenarios().isEmpty() || input.scenarios().size() > 100)
            throw new IllegalArgumentException("Baseline and 1 to 100 scenarios are required");
        Evaluation baseline = evaluateScenario(input.baseline());
        Set<String> names = new HashSet<>();
        names.add(baseline.name());
        List<Evaluation> evaluations = new ArrayList<>();
        List<Comparison> comparisons = new ArrayList<>();
        for (var scenario : input.scenarios()) {
            Evaluation evaluated = evaluateScenario(scenario);
            if (!names.add(evaluated.name())) throw new IllegalArgumentException("Scenario names must be unique");
            evaluations.add(evaluated);
            var original = baseline.result().decision();
            var changed = evaluated.result().decision();
            comparisons.add(new Comparison(evaluated.name(), original.selectedRoute(), changed.selectedRoute(),
                !Objects.equals(original.selectedRoute(), changed.selectedRoute()), changed.reasons(), changes(input.baseline(), scenario)));
        }
        return new Response("SIMULATED", false, "Caller-supplied hypothetical estimates; no inference or live estimator calls",
                            baseline, List.copyOf(evaluations), List.copyOf(comparisons));
    }
    private Evaluation evaluateScenario(WhatIfRequest.Scenario scenario) {
        if (scenario == null || scenario.name() == null || scenario.name().isBlank()
                || scenario.request() == null || scenario.request().task() == null
                || scenario.request().device() == null || scenario.costs() == null)
            throw new IllegalArgumentException("Scenario name, task, device, network, estimates and costs are required");
        long start = System.nanoTime();
        var request = scenario.request();
        DecisionRequestValidator.validate(request, config);
        var profile = request.task().priorityProfile();
        config.weights(profile);
        var device = request.device();
        double readiness = DeviceReadinessScorer.calculateReadiness(device.cpuPercent(), device.ramPercent(), null, null);
        var routes = RouteEvaluationBuilder.build(request, scenario.estimates(), scenario.costs(), config);
        var routing = RoutingEngine.evaluate(routes, request.currentRoute(), readiness, config, profile);
        var decision = DecisionResponseAssembler.assemble(request.task().taskId(), routing, scenario.estimates(),
                scenario.communicationCosts(), scenario.switchingCosts(), (System.nanoTime()-start)/1e6);
        return new Evaluation(scenario.name(), "SIMULATED", simulated(scenario),
            DetailedDecisionResponseAssembler.assemble(decision, routing, routes, readiness, config, profile));
    }

    private static List<InputChange> changes(WhatIfRequest.Scenario baseline, WhatIfRequest.Scenario scenario) {
        List<InputChange> changes = new ArrayList<>();
        var a = baseline.request(); var b = scenario.request();
        changed(changes, "task.deadlineMs", a.task().deadlineMs(), b.task().deadlineMs());
        changed(changes, "task.priorityProfile", a.task().priorityProfile(), b.task().priorityProfile());
        changed(changes, "task.privacyLevel", a.task().privacyLevel(), b.task().privacyLevel());
        changed(changes, "task.complexity", a.task().complexity(), b.task().complexity());
        changed(changes, "task.inputDataSizeBytes", a.task().inputDataSizeBytes(), b.task().inputDataSizeBytes());
        changed(changes, "device.cpuPercent", a.device().cpuPercent(), b.device().cpuPercent());
        changed(changes, "device.ramPercent", a.device().ramPercent(), b.device().ramPercent());
        changed(changes, "device.batteryPercent", a.device().batteryPercent(), b.device().batteryPercent());
        changed(changes, "device.batteryAvailable", a.device().batteryAvailable(), b.device().batteryAvailable());
        changed(changes, "network.connected", a.network().connected(), b.network().connected());
        changed(changes, "network.latencyMs", a.network().latencyMs(), b.network().latencyMs());
        changed(changes, "network.bandwidthMbps", a.network().bandwidthMbps(), b.network().bandwidthMbps());
        changed(changes, "currentRoute", a.currentRoute(), b.currentRoute());
        changed(changes, "estimates", baseline.estimates().estimates(), scenario.estimates().estimates());
        changed(changes, "costs", baseline.costs(), scenario.costs());
        changed(changes, "communicationCosts", baseline.communicationCosts(), scenario.communicationCosts());
        changed(changes, "switchingCosts", baseline.switchingCosts(), scenario.switchingCosts());
        return List.copyOf(changes);
    }
    private static void changed(List<InputChange> output, String name, Object a, Object b) {
        if (!Objects.equals(a, b)) output.add(new InputChange(name, a, b));
    }
    private static WhatIfRequest.Scenario simulated(WhatIfRequest.Scenario input) {
        Map<String, RouteEstimate> estimates = new HashMap<>();
        input.estimates().estimates().forEach((route, e) -> {
            if (e != null) estimates.put(route, new RouteEstimate(e.estimateAvailable(), e.reason(), e.estimatedLatencyMs(),
                e.energyEstimateJ(), e.deviceEnergyEstimateJ(), e.cloudEnergyEstimateJ(), e.transferEnergyEstimateJ(),
                e.carbonEstimateG(), e.uploadBytes(), e.downloadBytes(), e.communicationMb(), e.resourcePressure(), "SIMULATED"));
        });
        return new WhatIfRequest.Scenario(input.name(), input.request(), new EstimatorResponse(
            input.estimates().requestId(), input.estimates().calibrationVersion(), "SIMULATED", Map.copyOf(estimates)),
            input.costs(), input.communicationCosts(), input.switchingCosts());
    }
}
