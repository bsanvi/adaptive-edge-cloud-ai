
package com.adaptive.decision.rules;

public class NetworkRules {

    public static final double MAX_LATENCY_MS = 200.0;
    public static final double MIN_BANDWIDTH_MBPS = 5.0;

    public static boolean isRouteAllowed(
            String route,
            boolean connected,
            double latencyMs,
            double bandwidthMbps) {

        if (route == null) {
            return false;
        }

        String location = route.trim().toUpperCase();

        if (location.equals("EDGE")) {
            return true;
        }

        if (!location.equals("CLOUD")
                && !location.equals("HYBRID")) {
            return false;
        }

        // Cloud and Hybrid require a network connection.
        if (!connected) {
            return false;
        }

        // Reject invalid network measurements.
        if (!Double.isFinite(latencyMs)
                || !Double.isFinite(bandwidthMbps)
                || latencyMs < 0
                || bandwidthMbps <= 0) {
            return false;
        }

        // Reject slow network conditions.
        if (latencyMs > MAX_LATENCY_MS) {
            return false;
        }

        if (bandwidthMbps < MIN_BANDWIDTH_MBPS) {
            return false;
        }

        return true;
    }
}
