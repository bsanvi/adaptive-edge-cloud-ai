package com.adaptive.decision;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SupportingInputsGateTests {
    final DecisionIntegrationPolicy policy = new DecisionIntegrationPolicy(new DecisionPolicyConfig(
        128, 128, 500, 400, WhatIfTests.ZERO, WhatIfTests.ZERO), WhatIfTests.ZERO);
    DecisionRequest request(Instant time) {
        var request = WhatIfTests.scenario("input", "PUBLIC", true, 1000, 30, "BALANCED").request();
        return new DecisionRequest(request.task(), new DeviceMetrics(time, 30, 40, 50, true), request.network(), null);
    }
    SupportingInputsGate gate(Instant time, String source, boolean enabled) {
        return new SupportingInputsGate(enabled, Optional.of(policy),
            Optional.of(r -> new NetworkMeasurementMetadata(time, source)),
            Optional.of(r -> new ExecutionCapabilities(true, true)),
            Optional.of(r -> new DeviceProvenanceProvider.Provenance(time, source)));
    }
    @Test void activatesOnlyWithMeasuredFreshInputsAndExplicitPolicy() {
        var time = Instant.now().minusMillis(100);
        var result = gate(time, "MEASURED", true).resolve(request(time));
        assertEquals("MEASURED", result.estimatorInputs().deviceSource());
        assertEquals(time, result.estimatorInputs().networkSampledAt());
        assertTrue(result.estimatorInputs().edgeCapable());
        assertEquals(WhatIfTests.ZERO, result.routeCostInputs().monetaryCosts());
    }
    @Test void blocksMissingDisabledSimulatedStaleOrMismatchedInputs() {
        var time = Instant.now().minusMillis(100);
        assertThrows(IllegalStateException.class, () -> gate(time, "MEASURED", false).resolve(request(time)));
        assertThrows(IllegalStateException.class, () -> gate(time, "SIMULATED", true).resolve(request(time)));
        var stale = Instant.now().minusSeconds(10);
        assertThrows(IllegalStateException.class, () -> gate(stale, "MEASURED", true).resolve(request(stale)));
        assertThrows(IllegalStateException.class, () -> gate(time, "MEASURED", true).resolve(request(time.minusSeconds(1))));
        var missing = new SupportingInputsGate(true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThrows(IllegalStateException.class, () -> missing.resolve(request(time)));
    }
}
