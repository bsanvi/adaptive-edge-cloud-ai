
package com.adaptive.decision.service;

import java.util.List;

public record RoutingResult(
    String status,
    String selectedRoute,
    List<RouteResult> evaluations
) {

    public record RouteResult(
        String route,
        boolean feasible,
        double score,
        List<String> rejectionReasons
    ) {}
}
