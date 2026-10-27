
package com.adaptive.decision;

import com.adaptive.decision.service.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NetworkMeasurementMetadataTests {

    @Test
    void acceptsMeasuredNetworkMetadata() {

        var metadata = new NetworkMeasurementMetadata(
            Instant.now().minusSeconds(1),
            "MEASURED"
        );

        assertEquals("MEASURED", metadata.source());
        assertNotNull(metadata.sampledAt());
    }

    @Test
    void acceptsSimulatedNetworkMetadata() {

        var metadata = new NetworkMeasurementMetadata(
            Instant.now().minusSeconds(1),
            "SIMULATED"
        );

        assertEquals("SIMULATED", metadata.source());
    }

    @Test
    void rejectsMissingTimestamp() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new NetworkMeasurementMetadata(
                null,
                "MEASURED"
            )
        );
    }

    @Test
    void rejectsFutureTimestamp() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new NetworkMeasurementMetadata(
                Instant.now().plusSeconds(3600),
                "MEASURED"
            )
        );
    }

    @Test
    void rejectsInvalidSource() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new NetworkMeasurementMetadata(
                Instant.now().minusSeconds(1),
                "UNKNOWN"
            )
        );
    }

    @Test
    void unavailableProviderFailsExplicitly() {

        var provider =
            new UnconfiguredNetworkMetadataProvider();

        assertThrows(
            IllegalStateException.class,
            () -> provider.resolve(null)
        );
    }
}
