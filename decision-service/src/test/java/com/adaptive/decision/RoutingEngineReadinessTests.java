
package com.adaptive.decision;

import com.adaptive.decision.service.*;
import com.adaptive.decision.scoring.WeightedScorer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutingEngineReadinessTests {

    private List<RoutingEngine.RouteInput>
            testRoutes() {

        return List.of(

            new RoutingEngine.RouteInput(
                "EDGE",
                true,
                new WeightedScorer.Metrics(
                    1000.0,
                    0.0,
                    0.0,
                    0.0
                ),
                List.of()
            ),

            new RoutingEngine.RouteInput(
                "CLOUD",
                true,
                new WeightedScorer.Metrics(
                    1500.0,
                    0.0,
                    0.0,
                    0.0
                ),
                List.of()
            )
        );
    }

    @Test
    void baseScoringPrefersEdge() {

        var result = RoutingEngine.evaluate(
            testRoutes(),
            null
        );

        assertEquals(
            "EDGE",
            result.selectedRoute()
        );
    }

    @Test
    void lowDeviceReadinessPrefersCloud() {

        var result = RoutingEngine.evaluate(
            testRoutes(),
            null,
            0.0
        );

        assertEquals(
            "CLOUD",
            result.selectedRoute()
        );
    }

    @Test
    void highDeviceReadinessPrefersEdge() {

        var result = RoutingEngine.evaluate(
            testRoutes(),
            null,
            1.0
        );

        assertEquals(
            "EDGE",
            result.selectedRoute()
        );
    }

    @Test
    void cloudScoreRemainsUnchanged() {

        var result = RoutingEngine.evaluate(
            testRoutes(),
            null,
            0.0
        );

        var cloudEvaluation =
            result.evaluations()
                .stream()
                .filter(
                    e -> e.route().equals("CLOUD")
                )
                .findFirst()
                .orElseThrow();

        assertEquals(
            0.85,
            cloudEvaluation.score(),
            1e-9
        );
    }
}
