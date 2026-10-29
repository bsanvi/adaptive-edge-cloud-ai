
package com.adaptive.decision.dto;

import java.time.Instant;

public record EstimatorInputs(
    long estimatedFeatureBytes,
    long estimatedResultBytes,
    boolean edgeCapable,
    boolean cloudCapable,
    double edgeCarbonIntensityGPerKwh,
    double cloudCarbonIntensityGPerKwh,
    Instant networkSampledAt,
    String deviceSource,
    String networkSource
) {
}
