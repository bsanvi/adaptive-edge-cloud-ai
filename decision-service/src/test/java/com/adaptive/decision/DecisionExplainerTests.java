
package com.adaptive.decision;

import com.adaptive.decision.service.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecisionExplainerTests {

    @Test
    void explainsSelectedRoute() {

        RoutingResult result = new RoutingResult(
            "ROUTE_SELECTED",
            "EDGE",
            List.of(
                new RoutingResult.RouteResult(
                    "EDGE", true, 0.85, List.of()
                )
            )
        );

        var reasons = DecisionExplainer.explain(result);

        assertTrue(
            reasons.get(0).contains("EDGE selected")
        );
    }

    @Test
    void explainsPrivacyRejection() {

        RoutingResult result = new RoutingResult(
            "ROUTE_SELECTED",
            "EDGE",
            List.of(
                new RoutingResult.RouteResult(
                    "EDGE", true, 0.85, List.of()
                ),
                new RoutingResult.RouteResult(
                    "CLOUD", false, 0.0,
                    List.of("PRIVACY_RESTRICTION")
                )
            )
        );

        var reasons = DecisionExplainer.explain(result);

        assertTrue(
            reasons.contains(
                "CLOUD rejected: PRIVACY_RESTRICTION"
            )
        );
    }

    @Test
    void explainsNoFeasibleRoute() {

        RoutingResult result = new RoutingResult(
            "NO_FEASIBLE_ROUTE",
            null,
            List.of(
                new RoutingResult.RouteResult(
                    "EDGE", false, 0.0,
                    List.of("DEADLINE_EXCEEDED")
                )
            )
        );

        var reasons = DecisionExplainer.explain(result);

        assertTrue(
            reasons.get(0).contains("No execution route")
        );
    }

    @Test
    void rejectsNullResult() {

        assertThrows(
            IllegalArgumentException.class,
            () -> DecisionExplainer.explain(null)
        );
    }

    @Test
    void rejectsUnknownStatus() {

        RoutingResult result = new RoutingResult(
            "UNKNOWN",
            null,
            List.of()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> DecisionExplainer.explain(result)
        );
    }
}
