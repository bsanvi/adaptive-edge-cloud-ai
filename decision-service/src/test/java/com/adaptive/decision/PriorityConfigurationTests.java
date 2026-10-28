package com.adaptive.decision;
import com.adaptive.decision.scoring.*;
import com.adaptive.decision.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PriorityConfigurationTests {
    @TempDir Path temp;
    @Test void allProfilesSumToOneAndContributionsMatch() {
        var config = ScoringConfiguration.defaults();
        assertEquals(Map.of("time", .5, "energy", .2, "cost", .2, "carbon", .1), config.weights("BALANCED"));
        for (var profile : config.profiles().keySet()) {
            assertEquals(1, config.weights(profile).values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
            var result = WeightedScorer.score(new WeightedScorer.Metrics(100, 20, .3, .01), config, profile);
            assertEquals(result.score(), result.contributions().values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
        }
    }
    @Test void preferenceChangesWinnerWithoutChangingFeasibility() {
        var config = ScoringConfiguration.defaults();
        var inputs = List.of(
            new RoutingEngine.RouteInput("EDGE", true, new WeightedScorer.Metrics(100, 90, .9, .09), List.of()),
            new RoutingEngine.RouteInput("CLOUD", true, new WeightedScorer.Metrics(4000, 1, .01, .01), List.of()),
            new RoutingEngine.RouteInput("HYBRID", false, null, List.of("PRIVACY_RESTRICTED")));
        assertEquals("EDGE", RoutingEngine.evaluate(inputs, null, null, config, "LATENCY_PRIORITY").selectedRoute());
        for (var profile : List.of("ENERGY_PRIORITY", "COST_PRIORITY")) {
            var result = RoutingEngine.evaluate(inputs, null, null, config, profile);
            assertEquals("CLOUD", result.selectedRoute());
            assertFalse(result.evaluations().get(2).feasible());
        }
    }
    @Test void rejectsInvalidConfigurationAndUnknownProfiles() {
        var config = ScoringConfiguration.defaults();
        var profiles = new HashMap<>(config.profiles());
        profiles.put("BALANCED", Map.of("time", .9, "energy", .2, "cost", .2, "carbon", .1));
        assertThrows(IllegalArgumentException.class, () -> new ScoringConfiguration(config.bounds(), profiles));
        assertThrows(IllegalArgumentException.class, () -> new ScoringConfiguration.Bounds(2, 1));
        assertThrows(IllegalArgumentException.class, () -> config.weights("TYPO"));
    }
    @Test void loadsPythonSchemaAndCustomBounds() throws Exception {
        String bundled = Files.readString(Path.of("src/main/resources/decision-config.json"));
        Path file = temp.resolve("config.json");
        Files.writeString(file, bundled.replace("5000", "10000"));
        var config = ScoringConfiguration.load(file.toString());
        assertEquals(.5, WeightedScorer.score(new WeightedScorer.Metrics(5000, 0, 0, 0), config, "BALANCED").components().get("time"));
        Files.writeString(file, bundled.replace("0.5", "0.9"));
        assertThrows(IllegalArgumentException.class, () -> ScoringConfiguration.load(file.toString()));
    }

    @Test void loadsUnmodifiedPythonConfiguration() {
        var config = ScoringConfiguration.load("../decision-engine/config/config.json");
        assertEquals(ScoringConfiguration.defaults().bounds(), config.bounds());
        assertEquals(ScoringConfiguration.defaults().profiles(), config.profiles());
    }
    @Test void customConfigurationIsSharedByPipelineAndDetailedExplanation() {
        var defaults = ScoringConfiguration.defaults();
        var bounds = new HashMap<>(defaults.bounds());
        bounds.put("time", new ScoringConfiguration.Bounds(0, 10000));
        var profiles = new HashMap<>(defaults.profiles());
        profiles.put("BALANCED", defaults.weights("LATENCY_PRIORITY"));
        var custom = new ScoringConfiguration(bounds, profiles);
        var scenario = WhatIfTests.scenario("test", "PUBLIC", true, 1000, 100, "BALANCED");
        var gateway = org.mockito.Mockito.mock(EstimationGateway.class);
        org.mockito.Mockito.when(gateway.estimate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(scenario.estimates());
        var result = new DecisionPipeline(gateway, custom).evaluateDetailed(scenario.request(),
            new com.adaptive.decision.dto.EstimatorInputs(128, 128, true, true, 500, 400,
                java.time.Instant.now(), "SIMULATED", "SIMULATED"), scenario.costs());
        assertEquals(3, result.routeInputs().size());
        assertEquals(0, result.deviceReadiness());
        var decision = DecisionResponseAssembler.assemble("research", result.routing(), result.estimates(),
            scenario.communicationCosts(), scenario.switchingCosts(), 1);
        var detailed = DetailedDecisionResponseAssembler.assemble(decision, result.routing(), result.routeInputs(),
            result.deviceReadiness(), result.scoringConfiguration(), result.priorityProfile());
        assertEquals(.95, detailed.details().get(0).scoring().components().get("time"));
        assertEquals(.8, detailed.details().get(0).scoring().weights().get("time"));
        assertEquals(decision.evaluations().get(0).score(), detailed.details().get(0).scoring().adjustedScore());
    }
    @Test void allPreferencesPreserveHardPrivacyAndDeadlineConstraints() {
        var service = new WhatIfService(ScoringConfiguration.defaults());
        for (String profile : ScoringConfiguration.defaults().profiles().keySet()) {
            var baseline = WhatIfTests.scenario("private", "PRIVATE", true, 1000, 100, profile);
            var impossible = WhatIfTests.scenario("deadline", "PUBLIC", true, 50, 100, profile);
            var result = service.evaluate(new com.adaptive.decision.dto.WhatIfRequest(baseline, List.of(impossible)));
            assertEquals("EDGE", result.baseline().result().decision().selectedRoute());
            assertNull(result.scenarios().get(0).result().decision().selectedRoute());
        }
    }
}
