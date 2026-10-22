package com.team.aecai.orchestrator.integration;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class IntegratedContractTest {
    @Test void estimatorResponseMapsToPythonDecisionContract() {
        Map<String, Object> estimator = Map.of(
                "EDGE", Map.of("estimateAvailable", true,
                        "estimatedLatencyMs", 20.0,
                        "energyEstimateJ", 3.0,
                        "carbonEstimateG", 0.0005,
                        "communicationMb", 0.0),
                "CLOUD", Map.of("estimateAvailable", false),
                "HYBRID", Map.of("estimateAvailable", false));
        Map<String, Object> converted = IntegratedOrchestrator.decisionEstimates(estimator, 0.02);
        assertEquals(1, converted.size());
        assertTrue(converted.containsKey("edge"));
        assertEquals(20.0, ((Number)((Map<?, ?>)converted.get("edge"))
                .get("estimated_time_ms")).doubleValue());
        assertFalse(converted.containsKey("cloud"));
    }

    @Test void invalidOrUnsafeInputsRejected() {
        var task = new IntegratedOrchestrator.RunTask("test", List.of(1.0, 2.0),
                1000, true, "LOW", null);
        assertThrows(IllegalArgumentException.class,
                () -> IntegratedOrchestrator.validate(task));
        assertThrows(IllegalArgumentException.class,
                () -> IntegratedOrchestrator.complexityScore("UNKNOWN"));
    }

    @Test void availableRouteCheckIsExplicit() {
        assertFalse(IntegratedOrchestrator.isAvailable(Map.of(), "EDGE"));
        assertTrue(IntegratedOrchestrator.isAvailable(
                Map.of("EDGE", Map.of("estimateAvailable", true)), "EDGE"));
    }
}
