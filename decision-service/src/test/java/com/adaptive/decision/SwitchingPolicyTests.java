
package com.adaptive.decision;

import com.adaptive.decision.selection.RouteSelector.Candidate;
import com.adaptive.decision.selection.SwitchingPolicy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwitchingPolicyTests {

    @Test
    void keepsPreviousRouteForSmallImprovement() {

        String result =
            SwitchingPolicy.selectWithHysteresis(
                List.of(
                    new Candidate("EDGE", true, 0.85),
                    new Candidate("CLOUD", true, 0.87),
                    new Candidate("HYBRID", true, 0.80)
                ),
                "EDGE"
            );

        assertEquals("EDGE", result);
    }

    @Test
    void switchesForLargeImprovement() {

        String result =
            SwitchingPolicy.selectWithHysteresis(
                List.of(
                    new Candidate("EDGE", true, 0.70),
                    new Candidate("CLOUD", true, 0.90),
                    new Candidate("HYBRID", true, 0.80)
                ),
                "EDGE"
            );

        assertEquals("CLOUD", result);
    }

    @Test
    void neverKeepsInfeasiblePreviousRoute() {

        String result =
            SwitchingPolicy.selectWithHysteresis(
                List.of(
                    new Candidate("EDGE", false, 0.95),
                    new Candidate("CLOUD", true, 0.80),
                    new Candidate("HYBRID", false, 0.90)
                ),
                "EDGE"
            );

        assertEquals("CLOUD", result);
    }

    @Test
    void selectsBestWhenNoPreviousRoute() {

        String result =
            SwitchingPolicy.selectWithHysteresis(
                List.of(
                    new Candidate("EDGE", true, 0.70),
                    new Candidate("CLOUD", true, 0.90),
                    new Candidate("HYBRID", true, 0.80)
                ),
                null
            );

        assertEquals("CLOUD", result);
    }

    @Test
    void returnsNullWhenNoRouteIsFeasible() {

        String result =
            SwitchingPolicy.selectWithHysteresis(
                List.of(
                    new Candidate("EDGE", false, 0.70),
                    new Candidate("CLOUD", false, 0.90),
                    new Candidate("HYBRID", false, 0.80)
                ),
                "EDGE"
            );

        assertNull(result);
    }

    @Test
    void rejectsInvalidPreviousRoute() {

        assertThrows(
            IllegalArgumentException.class,
            () -> SwitchingPolicy.selectWithHysteresis(
                List.of(
                    new Candidate("EDGE", true, 0.80)
                ),
                "UNKNOWN"
            )
        );
    }

    @Test
    void keepsPreviousRouteAtThreshold() {

        String result =
            SwitchingPolicy.selectWithHysteresis(
                List.of(
                    new Candidate("EDGE", true, 0.75),
                    new Candidate("CLOUD", true, 0.80)
                ),
                "EDGE"
            );

        assertEquals("EDGE", result);
    }
}
