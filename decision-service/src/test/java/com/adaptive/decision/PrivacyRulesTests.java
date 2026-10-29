
package com.adaptive.decision;

import com.adaptive.decision.rules.PrivacyRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyRulesTests {

    @Test
    void privateTasksAllowEdge() {
        assertTrue(
            PrivacyRules.isRouteAllowed("PRIVATE", "EDGE")
        );
    }

    @Test
    void privateTasksRejectCloud() {
        assertFalse(
            PrivacyRules.isRouteAllowed("PRIVATE", "CLOUD")
        );
    }

    @Test
    void privateTasksRejectHybrid() {
        assertFalse(
            PrivacyRules.isRouteAllowed("PRIVATE", "HYBRID")
        );
    }

    @Test
    void publicTasksAllowAllRoutes() {
        assertTrue(
            PrivacyRules.isRouteAllowed("PUBLIC", "EDGE")
        );

        assertTrue(
            PrivacyRules.isRouteAllowed("PUBLIC", "CLOUD")
        );

        assertTrue(
            PrivacyRules.isRouteAllowed("PUBLIC", "HYBRID")
        );
    }

    @Test
    void unknownPrivacyLevelsAreRejected() {
        assertFalse(
            PrivacyRules.isRouteAllowed("UNKNOWN", "CLOUD")
        );

        assertFalse(
            PrivacyRules.isRouteAllowed(null, "EDGE")
        );
    }

    @Test
    void invalidRouteIsRejected() {
        assertFalse(
            PrivacyRules.isRouteAllowed("PUBLIC", "INVALID")
        );
    }
}
