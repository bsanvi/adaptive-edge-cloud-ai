
package com.adaptive.decision.dto;

import java.util.Map;

public record EstimatorResponse(
    String requestId,
    String calibrationVersion,
    String calibrationSource,
    Map<String, RouteEstimate> estimates
) {
}
