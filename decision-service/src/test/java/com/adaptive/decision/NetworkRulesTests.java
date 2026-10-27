
package com.adaptive.decision;

import com.adaptive.decision.rules.NetworkRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkRulesTests {

    @Test
    void edgeWorksWithoutNetwork() {
        assertTrue(
            NetworkRules.isRouteAllowed(
                "EDGE", false, 0.0, 0.0
            )
        );
    }

    @Test
    void cloudRejectedWithoutConnection() {
        assertFalse(
            NetworkRules.isRouteAllowed(
                "CLOUD", false, 20.0, 50.0
            )
        );
    }

    @Test
    void hybridRejectedWithoutConnection() {
        assertFalse(
            NetworkRules.isRouteAllowed(
                "HYBRID", false, 20.0, 50.0
            )
        );
    }

    @Test
    void cloudAllowedOnHealthyNetwork() {
        assertTrue(
            NetworkRules.isRouteAllowed(
                "CLOUD", true, 30.0, 50.0
            )
        );
    }

    @Test
    void hybridAllowedOnHealthyNetwork() {
        assertTrue(
            NetworkRules.isRouteAllowed(
                "HYBRID", true, 30.0, 50.0
            )
        );
    }

    @Test
    void highLatencyRejectsRemoteRoutes() {
        assertFalse(
            NetworkRules.isRouteAllowed(
                "CLOUD", true, 350.0, 50.0
            )
        );
    }

    @Test
    void lowBandwidthRejectsRemoteRoutes() {
        assertFalse(
            NetworkRules.isRouteAllowed(
                "HYBRID", true, 20.0, 2.0
            )
        );
    }

    @Test
    void thresholdBoundaryIsAccepted() {
        assertTrue(
            NetworkRules.isRouteAllowed(
                "CLOUD", true, 200.0, 5.0
            )
        );
    }

    @Test
    void invalidMeasurementsAreRejected() {
        assertFalse(
            NetworkRules.isRouteAllowed(
                "CLOUD",
                true,
                Double.NaN,
                50.0
            )
        );

        assertFalse(
            NetworkRules.isRouteAllowed(
                "HYBRID",
                true,
                20.0,
                Double.POSITIVE_INFINITY
            )
        );
    }

    @Test
    void invalidRouteIsRejected() {
        assertFalse(
            NetworkRules.isRouteAllowed(
                "UNKNOWN", true, 20.0, 50.0
            )
        );
    }
}
