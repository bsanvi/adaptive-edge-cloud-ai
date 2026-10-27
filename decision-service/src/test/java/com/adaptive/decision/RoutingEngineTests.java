
package com.adaptive.decision;

import com.adaptive.decision.service.RoutingEngine;
import com.adaptive.decision.scoring.WeightedScorer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutingEngineTests {

    private RoutingEngine.RouteInput route(
            String name,
            boolean feasible,
            double timeMs) {

        return new RoutingEngine.RouteInput(
            name,
            feasible,
            new WeightedScorer.Metrics(
                timeMs,
                100.0,
                0.1,
                1.0
            ),
            feasible
                ? List.of()
                : List.of("PRIVACY_RESTRICTION")
        );
    }

    @Test
    void selectsHighestScoringFeasibleRoute() {

        var result = RoutingEngine.evaluate(
            List.of(
                route("EDGE", true, 5000.0),
                route("CLOUD", true, 1000.0),
                route("HYBRID", true, 3000.0)
            ),
            null
        );

        assertEquals("ROUTE_SELECTED", result.status());
        assertEquals("CLOUD", result.selectedRoute());
        assertEquals(3, result.evaluations().size());
    }

    @Test
    void neverSelectsInfeasibleRoute() {

        var result = RoutingEngine.evaluate(
            List.of(
                route("EDGE", true, 5000.0),
                route("CLOUD", false, 100.0),
                route("HYBRID", false, 1000.0)
            ),
            null
        );

        assertEquals("EDGE", result.selectedRoute());
    }

    @Test
    void returnsNoFeasibleRoute() {

        var result = RoutingEngine.evaluate(
            List.of(
                route("EDGE", false, 5000.0),
                route("CLOUD", false, 1000.0),
                route("HYBRID", false, 3000.0)
            ),
            null
        );

        assertEquals(
            "NO_FEASIBLE_ROUTE",
            result.status()
        );

        assertNull(result.selectedRoute());
    }

    @Test
    void appliesHysteresis() {

        var result = RoutingEngine.evaluate(
            List.of(
                route("EDGE", true, 1000.0),
                route("CLOUD", true, 900.0)
            ),
            "EDGE"
        );

        assertEquals("EDGE", result.selectedRoute());
    }
}
