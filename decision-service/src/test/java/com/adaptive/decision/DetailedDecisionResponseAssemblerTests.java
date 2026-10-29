
package com.adaptive.decision;

import com.adaptive.decision.dto.*;
import com.adaptive.decision.scoring.WeightedScorer;
import com.adaptive.decision.service.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DetailedDecisionResponseAssemblerTests {

    private List<RoutingEngine.RouteInput> inputs() {

        return List.of(
            new RoutingEngine.RouteInput(
                "EDGE",
                true,
                new WeightedScorer.Metrics(
                    2500.0,
                    50.0,
                    0.5,
                    0.05
                ),
                List.of()
            ),

            new RoutingEngine.RouteInput(
                "CLOUD",
                false,
                null,
                List.of("NETWORK_RESTRICTION")
            )
        );
    }

    private DecisionResponse decision(
            RoutingResult routing) {

        return new DecisionResponse(
            "task-001",
            routing.status(),
            routing.selectedRoute(),
            5.0,
            List.of(),
            List.of("EDGE selected")
        );
    }

    @Test
    void buildsScoreBreakdownForFeasibleRoute() {

        var routeInputs = inputs();

        var routing = RoutingEngine.evaluate(
            routeInputs,
            null,
            0.6
        );

        var detailed =
            DetailedDecisionResponseAssembler.assemble(
                decision(routing),
                routing,
                routeInputs,
                0.6
            );

        var edge = detailed.details().get(0);

        assertEquals("EDGE", edge.route());
        assertTrue(edge.feasible());
        assertNotNull(edge.scoring());

        assertEquals(
            0.5,
            edge.scoring().baseScore(),
            1e-9
        );

        assertEquals(
            0.51,
            edge.scoring().adjustedScore(),
            1e-9
        );

        assertEquals(
            0.6,
            edge.scoring().deviceReadiness(),
            1e-9
        );

        assertEquals(
            0.25,
            edge.scoring()
                .contributions()
                .get("time"),
            1e-9
        );
    }

    @Test
    void preservesInfeasibleRouteReasons() {

        var routeInputs = inputs();

        var routing = RoutingEngine.evaluate(
            routeInputs,
            null,
            0.6
        );

        var detailed =
            DetailedDecisionResponseAssembler.assemble(
                decision(routing),
                routing,
                routeInputs,
                0.6
            );

        var cloud = detailed.details().get(1);

        assertFalse(cloud.feasible());
        assertNull(cloud.scoring());

        assertTrue(
            cloud.rejectionReasons().contains(
                "NETWORK_RESTRICTION"
            )
        );
    }

    @Test
    void rejectsMismatchedDecision() {

        var routeInputs = inputs();

        var routing = RoutingEngine.evaluate(
            routeInputs,
            null,
            0.6
        );

        var incorrectDecision = new DecisionResponse(
            "task-001",
            "NO_FEASIBLE_ROUTE",
            null,
            5.0,
            List.of(),
            List.of()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> DetailedDecisionResponseAssembler.assemble(
                incorrectDecision,
                routing,
                routeInputs,
                0.6
            )
        );
    }
}
