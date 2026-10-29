
package com.adaptive.decision.dto;

public record RouteEstimate(
    boolean estimateAvailable,
    String reason,
    Double estimatedLatencyMs,
    Double energyEstimateJ,
    Double deviceEnergyEstimateJ,
    Double cloudEnergyEstimateJ,
    Double transferEnergyEstimateJ,
    Double carbonEstimateG,
    Long uploadBytes,
    Long downloadBytes,
    Double communicationMb,
    Double resourcePressure,
    String source
) {
}
