package com.adaptive.decision;

import com.adaptive.decision.selection.RouteSelector;
import com.adaptive.decision.selection.RouteSelector.Candidate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteSelectorTests {

    @Test
    void selectsHighestScoringFeasibleRoute() {
        String result = RouteSelector.selectBest(List.of(
            new Candidate("EDGE", true, 0.75),
            new Candidate("CLOUD", true, 0.90),
            new Candidate("HYBRID", true, 0.80)
        ));

        assertEquals("CLOUD", result);
    }

    @Test
    void neverSelectsInfeasibleRoute() {
        String result = RouteSelector.selectBest(List.of(
            new Candidate("EDGE", true, 0.70),
            new Candidate("CLOUD", false, 0.99),
            new Candidate("HYBRID", false, 0.95)
        ));

        assertEquals("EDGE", result);
    }

    @Test
    void returnsNullWhenNothingIsFeasible() {
        String result = RouteSelector.selectBest(List.of(
            new Candidate("EDGE", false, 0.50),
            new Candidate("CLOUD", false, 0.90),
            new Candidate("HYBRID", false, 0.70)
        ));

        assertNull(result);
    }

    @Test
    void rejectsNullCandidates() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RouteSelector.selectBest(null)
        );
    }

    @Test
    void rejectsInvalidRouteNames() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RouteSelector.selectBest(List.of(
                new Candidate("UNKNOWN", true, 0.90)
            ))
        );
    }

    @Test
    void rejectsInvalidScores() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RouteSelector.selectBest(List.of(
                new Candidate("EDGE", true, Double.NaN)
            ))
        );
    }

    @Test
    void handlesEmptyCandidateList() {
        assertNull(RouteSelector.selectBest(List.of()));
    }
}
