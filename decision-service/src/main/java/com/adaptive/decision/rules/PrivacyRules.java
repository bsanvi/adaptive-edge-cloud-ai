
package com.adaptive.decision.rules;

public class PrivacyRules {

    public static boolean isRouteAllowed(
            String privacyLevel,
            String route) {

        if (privacyLevel == null || route == null) {
            return false;
        }

        String privacy = privacyLevel.trim().toUpperCase();
        String location = route.trim().toUpperCase();

        if (!location.equals("EDGE")
                && !location.equals("CLOUD")
                && !location.equals("HYBRID")) {
            return false;
        }

        if ((privacy.equals("PRIVATE") || privacy.equals("CRITICAL"))) {
            return location.equals("EDGE");
        }

        if ((privacy.equals("PUBLIC") || privacy.equals("NORMAL"))) {
            return true;
        }

        // Unknown privacy levels are rejected safely.
        return false;
    }
}
