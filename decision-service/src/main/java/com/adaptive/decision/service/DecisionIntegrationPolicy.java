package com.adaptive.decision.service;
import java.util.Map;
/** Explicit policies supplied by the integration owner; contains no measured telemetry. */
public record DecisionIntegrationPolicy(DecisionPolicyConfig policy, Map<String, Double> communicationCosts) {
    public DecisionIntegrationPolicy {
        if (policy == null || communicationCosts == null) throw new IllegalArgumentException("Integration policies required");
        communicationCosts = Map.copyOf(communicationCosts);
    }
}
