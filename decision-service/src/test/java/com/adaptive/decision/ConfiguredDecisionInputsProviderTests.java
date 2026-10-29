
package com.adaptive.decision;

import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguredDecisionInputsProviderTests {

    private DecisionPolicyConfig config() {
        return new DecisionPolicyConfig(
            256L,
            128L,
            500.0,
            400.0,
            Map.of(
                "EDGE", 0.1,
                "CLOUD", 0.2,
                "HYBRID", 0.15
            ),
            Map.of(
                "EDGE", 0.0,
                "CLOUD", 0.01,
                "HYBRID", 0.02
            )
        );
    }

    private DecisionRequest request() {
        return new DecisionRequest(
            new TaskProfile(
                "task-001",
                "SENSOR_ANOMALY",
                1024L,
                "LOW",
                1000.0,
                "PUBLIC",
                "BALANCED"
            ),
            new DeviceMetrics(
                Instant.now(),
                30.0,
                40.0,
                80.0,
                true
            ),
            new NetworkMetrics(
                true,
                20.0,
                50.0
            ),
            null
        );
    }

    @Test
    void buildsSupportingInputsFromProviders() {

        NetworkMeasurementMetadataProvider network =
            request -> new NetworkMeasurementMetadata(
                Instant.now().minusSeconds(1),
                "SIMULATED"
            );

        ExecutionCapabilitiesProvider capabilities =
            request -> new ExecutionCapabilities(
                true, true
            );

        var provider =
            new ConfiguredDecisionInputsProvider(
                config(),
                network,
                capabilities,
                "SIMULATED",
                Map.of(
                    "EDGE", 0.01,
                    "CLOUD", 0.02,
                    "HYBRID", 0.015
                )
);

        var result = provider.resolve(request());

        assertEquals(
            "SIMULATED",
            result.estimatorInputs().deviceSource()
        );

        assertEquals(
            3,
            result.communicationCosts().size()
        );

        assertEquals(
            256L,
            result.estimatorInputs()
                .estimatedFeatureBytes()
        );

        assertTrue(
            result.estimatorInputs().edgeCapable()
        );

        assertTrue(
            result.estimatorInputs().cloudCapable()
        );

        assertEquals(
            "SIMULATED",
            result.estimatorInputs().networkSource()
        );

        assertEquals(
            3,
            result.routeCostInputs()
                .monetaryCosts().size()
        );
    }

    @Test
    void rejectsMissingCapabilities() {

        NetworkMeasurementMetadataProvider network =
            request -> new NetworkMeasurementMetadata(
                Instant.now().minusSeconds(1),
                "SIMULATED"
            );

        ExecutionCapabilitiesProvider capabilities =
            request -> null;

        var provider =
            new ConfiguredDecisionInputsProvider(
                config(),
                network,
                capabilities,
                "SIMULATED",
                Map.of(
                    "EDGE", 0.01,
                    "CLOUD", 0.02,
                    "HYBRID", 0.015
                )
);

        assertThrows(
            IllegalStateException.class,
            () -> provider.resolve(request())
        );
    }

    @Test
    void rejectsIncompleteRequest() {

        NetworkMeasurementMetadataProvider network =
            request -> new NetworkMeasurementMetadata(
                Instant.now().minusSeconds(1),
                "SIMULATED"
            );

        ExecutionCapabilitiesProvider capabilities =
            request -> new ExecutionCapabilities(
                true, true
            );

        var provider =
            new ConfiguredDecisionInputsProvider(
                config(),
                network,
                capabilities,
                "SIMULATED",
                Map.of(
                    "EDGE", 0.01,
                    "CLOUD", 0.02,
                    "HYBRID", 0.015
                )
);

        assertThrows(
            IllegalArgumentException.class,
            () -> provider.resolve(null)
        );
    }

    
@Test
void rejectsMissingCommunicationCosts() {

    NetworkMeasurementMetadataProvider network =
        request -> new NetworkMeasurementMetadata(
            Instant.now().minusSeconds(1),
            "SIMULATED"
        );

    ExecutionCapabilitiesProvider capabilities =
        request -> new ExecutionCapabilities(
            true, true
        );

    assertThrows(
        IllegalArgumentException.class,
        () -> new ConfiguredDecisionInputsProvider(
            config(),
            network,
            capabilities,
            "SIMULATED",
            Map.of("EDGE", 0.01)
        )
    );
}

}
