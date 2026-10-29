package com.adaptive.decision;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DecisionResponseAssemblerTests {
    private EstimatorResponse estimates() {
        return new EstimatorResponse("task-001", "synthetic-v1", "SIMULATED",
            Map.of("EDGE", WhatIfTests.estimate(100, 10)));
    }

@Test
void preservesUnavailableRouteWithoutFabricatingValues() {

    RoutingResult routing = new RoutingResult(
        "NO_FEASIBLE_ROUTE",
        null,
        List.of(
            new RoutingResult.RouteResult(
                "HYBRID",
                false,
                0.0,
                List.of("ESTIMATE_UNAVAILABLE")
            )
        )
    );

    DecisionResponse response =
        DecisionResponseAssembler.assemble(
            "task-001",
            routing,
            estimates(),
            Map.of(),
            Map.of(),
            5.0
        );

    assertEquals(
        "NO_FEASIBLE_ROUTE",
        response.status()
    );

    assertNull(response.selectedRoute());

    RouteEvaluation evaluation =
        response.evaluations().get(0);

    assertFalse(evaluation.feasible());
    assertNull(evaluation.estimatedLatencyMs());
    assertNull(evaluation.estimatedEnergyJ());
    assertNull(evaluation.estimatedCarbonG());

    assertTrue(
        evaluation.rejectionReasons()
            .contains("ESTIMATE_UNAVAILABLE")
    );
}

}
