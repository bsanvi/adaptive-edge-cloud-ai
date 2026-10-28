package com.adaptive.decision;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.scoring.*;
import com.adaptive.decision.service.*;
import com.adaptive.decision.selection.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
class IndependentCompletionTests {
    final ScoringConfiguration defaults = ScoringConfiguration.defaults();
    ScoringConfiguration settings(double strength, double switching, double latency, double bandwidth) {
        return new ScoringConfiguration(defaults.bounds(), defaults.profiles(), strength, switching, latency, bandwidth);
    }
    @Test void configurableReadinessAndHysteresisAffectRoutingAndExplanationsTogether() {
        var scenario = WhatIfTests.scenario("ready", "PUBLIC", true, 1000, 0, "BALANCED");
        var request = new DecisionRequest(scenario.request().task(), scenario.request().device(), scenario.request().network(), "CLOUD");
        var inputs = RouteEvaluationBuilder.build(request, scenario.estimates(), scenario.costs());
        assertEquals("CLOUD", RoutingEngine.evaluate(inputs, "CLOUD", 1., settings(0, 0, 200, 5), "BALANCED").selectedRoute());
        var config = settings(.1, 0, 200, 5);
        var routing = RoutingEngine.evaluate(inputs, "CLOUD", 1., config, "BALANCED");
        assertEquals("EDGE", routing.selectedRoute());
        var response = DecisionResponseAssembler.assemble("research", routing, scenario.estimates(), WhatIfTests.ZERO, WhatIfTests.ZERO, 1);
        var details = DetailedDecisionResponseAssembler.assemble(response, routing, inputs, 1., config, "BALANCED");
        assertEquals(response.evaluations().get(0).score(), details.details().get(0).scoring().adjustedScore());
        assertEquals("CLOUD", RoutingEngine.evaluate(inputs, "CLOUD", 1., settings(.1, .05, 200, 5), "BALANCED").selectedRoute());
    }
    @Test void configurableNetworkLimitsAreIndependentOfProfile() {
        var scenario = WhatIfTests.scenario("test", "PUBLIC", true, 1000, 100, "BALANCED");
        var routes = RouteEvaluationBuilder.build(scenario.request(), scenario.estimates(), scenario.costs(), settings(.1, .05, 10, 5));
        assertTrue(routes.get(0).feasible()); assertFalse(routes.get(1).feasible()); assertFalse(routes.get(2).feasible());
        assertThrows(IllegalArgumentException.class, () -> settings(2, .05, 200, 5));
        assertThrows(IllegalArgumentException.class, () -> settings(.1, -.1, 200, 5));
        assertThrows(IllegalArgumentException.class, () -> settings(.1, .05, 200, 0));
    }
    @Test void inconsistentEstimatorCannotOverrideExecutionCapabilities() {
        var scenario = WhatIfTests.scenario("test", "PUBLIC", true, 1000, 0, "BALANCED");
        var gateway = mock(EstimationGateway.class);
        when(gateway.estimate(any(), any())).thenReturn(scenario.estimates());
        var result = new DecisionPipeline(gateway, defaults).evaluateDetailed(scenario.request(), new EstimatorInputs(
            128, 128, false, true, 500, 400, java.time.Instant.now(), "SIMULATED", "SIMULATED"), scenario.costs());
        assertEquals("CLOUD", result.routing().selectedRoute());
        assertFalse(result.routing().evaluations().get(0).feasible());
        assertFalse(result.routing().evaluations().get(2).feasible());
        assertTrue(result.routing().evaluations().get(0).rejectionReasons().contains("EXECUTION_CAPABILITY_UNAVAILABLE"));
    }
    @Test void whatIfReportsChangedInputsAndNormalizesIncomingSourceLabels() {
        var a = WhatIfTests.scenario("base", "PUBLIC", true, 1000, 100, "BALANCED");
        var b = WhatIfTests.scenario("changed", "PUBLIC", true, 100, 20, "ENERGY_PRIORITY");
        b = new WhatIfRequest.Scenario(b.name(), b.request(), new EstimatorResponse("research", "claimed-measured",
            "MEASURED", b.estimates().estimates()), b.costs(), b.communicationCosts(), b.switchingCosts());
        var response = new WhatIfService(defaults).evaluate(new WhatIfRequest(a, List.of(b)));
        var fields = response.comparisons().get(0).changes().stream().map(WhatIfService.InputChange::field).toList();
        assertTrue(fields.containsAll(List.of("task.deadlineMs", "task.priorityProfile", "device.cpuPercent", "device.ramPercent")));
        assertEquals("SIMULATED", response.scenarios().get(0).simulatedInputs().estimates().calibrationSource());
        assertEquals("MEASURED", b.estimates().calibrationSource());
    }
    @Test void whatIfRejectsInvalidTaskValuesRatherThanConvertingThemToNoFeasibleRoute() {
        var a = WhatIfTests.scenario("base", "PUBLIC", true, 1000, 100, "BALANCED");
        var invalid = WhatIfTests.scenario("invalid", "PUBLIC", true, -1, 100, "BALANCED");
        assertThrows(IllegalArgumentException.class, () -> new WhatIfService(defaults).evaluate(new WhatIfRequest(a, List.of(invalid))));
    }
    @Test void estimatorTimeoutsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new com.adaptive.decision.client.EstimatorClient(
            org.springframework.web.client.RestClient.builder(), "http://127.0.0.1", 0, 1));
    }
}
