
package com.adaptive.decision.service;

import java.time.Instant;
import java.util.Set;

public record NetworkMeasurementMetadata(
    Instant sampledAt,
    String source
) {

    private static final Set<String> VALID_SOURCES =
        Set.of(
            "MEASURED",
            "SIMULATED",
            "ESTIMATED",
            "CALIBRATED"
        );

    public NetworkMeasurementMetadata {

        if (sampledAt == null) {
            throw new IllegalArgumentException(
                "Network measurement timestamp is required"
            );
        }

        if (sampledAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                "Network measurement cannot be in the future"
            );
        }

        if (!VALID_SOURCES.contains(source)) {
            throw new IllegalArgumentException(
                "Invalid network measurement source"
            );
        }
    }
}
