package com.adaptive.decision;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;
import com.adaptive.decision.scoring.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WhatIfTests {
    static final Map<String, Double> ZERO = Map.of("EDGE", 0., "CLOUD", 0., "HYBRID", 0.);
    static RouteEstimate estimate(double latency, double energy) {
        return new RouteEstimate(true, null, latency, energy, energy, 0., 0., .001, 0L, 0L, 0., .1, "SIMULATED");
    }
    static WhatIfRequest.Scenario scenario(String name, String privacy, boolean connected,
            double deadline, double cpu, String profile) {
        var request = new DecisionRequest(new TaskProfile("research", "SENSOR_ANOMALY", 1024, "LOW", deadline, privacy, profile),
            new DeviceMetrics(Instant.parse("2026-10-28T00:00:00Z"), cpu, cpu, 50, true),
            new NetworkMetrics(connected, 20, 50), null);
        return new WhatIfRequest.Scenario(name, request,
            new EstimatorResponse("research", "synthetic-v1", "SIMULATED", Map.of(
                "EDGE", estimate(500, 20), "CLOUD", estimate(100, 40), "HYBRID", estimate(300, 50))),
            new RouteCostInputs(ZERO), ZERO, ZERO);
    }
    @Test void comparesPrivacyNetworkDeadlineAndDeviceScenarios() {
        var service = new WhatIfService(ScoringConfiguration.defaults());
        var baseline = scenario("normal", "PUBLIC", true, 1000, 100, "BALANCED");
        var response = service.evaluate(new WhatIfRequest(baseline, List.of(
            scenario("private", "PRIVATE", true, 1000, 100, "LATENCY_PRIORITY"),
            scenario("offline", "PUBLIC", false, 1000, 100, "BALANCED"),
            scenario("deadline", "PUBLIC", true, 50, 100, "BALANCED"),
            scenario("ready", "PUBLIC", true, 1000, 0, "BALANCED"))));
        assertEquals("SIMULATED", response.source());
        assertFalse(response.tasksExecuted());
        assertEquals("CLOUD", response.baseline().result().decision().selectedRoute());
        assertEquals("EDGE", response.scenarios().get(0).result().decision().selectedRoute());
        assertEquals("EDGE", response.scenarios().get(1).result().decision().selectedRoute());
        assertNull(response.scenarios().get(2).result().decision().selectedRoute());
        assertEquals("EDGE", response.scenarios().get(3).result().decision().selectedRoute());
        assertTrue(response.comparisons().stream().allMatch(WhatIfService.Comparison::routeChanged));
        assertEquals(100, baseline.request().device().cpuPercent());
    }
    @Test void rejectsMissingScenariosDuplicateNamesAndUnknownProfile() {
        var service = new WhatIfService(ScoringConfiguration.defaults());
        var baseline = scenario("normal", "PUBLIC", true, 1000, 100, "BALANCED");
        assertThrows(IllegalArgumentException.class, () -> service.evaluate(new WhatIfRequest(baseline, List.of())));
        assertThrows(IllegalArgumentException.class, () -> service.evaluate(new WhatIfRequest(baseline, List.of(baseline))));
        assertThrows(IllegalArgumentException.class, () -> service.evaluate(new WhatIfRequest(baseline,
            List.of(scenario("typo", "PUBLIC", true, 1000, 100, "UNKNOWN")))));
    }
    @Test void explanationsUseTheScoringProfile() {
        var scenario = scenario("latency", "PUBLIC", true, 1000, 100, "LATENCY_PRIORITY");
        var result = new WhatIfService(ScoringConfiguration.defaults()).evaluate(new WhatIfRequest(
            scenario, List.of(scenario("cost", "PUBLIC", true, 1000, 100, "COST_PRIORITY"))));
        assertEquals(.8, result.baseline().result().details().get(0).scoring().weights().get("time"));
    }
}
