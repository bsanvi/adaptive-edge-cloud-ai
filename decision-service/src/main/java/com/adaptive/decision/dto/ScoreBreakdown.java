
package com.adaptive.decision.dto;

import java.util.Map;

public record ScoreBreakdown(
    double baseScore,
    Double adjustedScore,
    Double deviceReadiness,
    Map<String, Double> components,
    Map<String, Double> contributions,
    Map<String, Double> weights
) {
}
