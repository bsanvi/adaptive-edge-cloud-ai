
package com.adaptive.decision;

import com.adaptive.decision.dto.*;
import com.adaptive.decision.scoring.DecisionWeights;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetailedDecisionResponseTests {

    @Test
    void storesScoringBreakdown() {

        var breakdown = new ScoreBreakdown(
            0.8,
            0.78,
            0.6,
            Map.of("time", 0.8),
            Map.of("time", 0.4),
            DecisionWeights.defaults()
        );

        assertEquals(0.8, breakdown.baseScore());
        assertEquals(0.78, breakdown.adjustedScore());
        assertEquals(0.6, breakdown.deviceReadiness());

        assertEquals(
            0.5,
            breakdown.weights().get("time")
        );
    }

    @Test
    void wrapsExistingDecisionResponse() {

        var original = new DecisionResponse(
            "task-001",
            "ROUTE_SELECTED",
            "EDGE",
            5.0,
            List.of(),
            List.of("EDGE selected")
        );

        var detailed = new DetailedDecisionResponse(
            original,
            List.of(
                new DetailedRouteEvaluation(
                    "EDGE",
                    true,
                    List.of(),
                    null
                )
            )
        );

        assertEquals(
            "EDGE",
            detailed.decision().selectedRoute()
        );

        assertEquals(1, detailed.details().size());
    }

    @Test
    void weightsSumToOne() {

        double total = DecisionWeights.defaults()
            .values()
            .stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        assertEquals(1.0, total, 1e-9);
    }
}
