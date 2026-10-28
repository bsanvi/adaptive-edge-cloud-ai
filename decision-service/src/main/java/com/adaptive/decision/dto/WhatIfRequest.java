package com.adaptive.decision.dto;
import java.util.List;
import java.util.Map;

/** Full snapshots prevent unreported overrides or reusing stale live estimates. */
public record WhatIfRequest(Scenario baseline, List<Scenario> scenarios) {
    public record Scenario(String name, DecisionRequest request, EstimatorResponse estimates,
                           RouteCostInputs costs, Map<String, Double> communicationCosts,
                           Map<String, Double> switchingCosts) {}
}
