package com.adaptive.decision;

import com.adaptive.decision.dto.*;
import com.adaptive.decision.scoring.*;
import com.adaptive.decision.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class DecisionAccuracyExperimentsTests {
    record Case(String expectedRoute, List<String> feasibleRoutes, String description, WhatIfRequest.Scenario scenario) {
        @Override public String toString() { return scenario.name(); }
    }
    static List<Case> cases() throws Exception {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        try (var input = DecisionAccuracyExperimentsTests.class.getResourceAsStream("/contracts/accuracy-cases.json")) {
            return Arrays.asList(mapper.readValue(input, Case[].class));
        }
    }
    static Stream<Case> caseSource() throws Exception { return cases().stream(); }
    static WhatIfService.Evaluation evaluate(Case test) {
        var control = WhatIfTests.scenario("control", "PUBLIC", true, 1000, 100, "BALANCED");
        return new WhatIfService(ScoringConfiguration.defaults()).evaluate(new WhatIfRequest(test.scenario(), List.of(control))).baseline();
    }
    @ParameterizedTest(name="{0}") @MethodSource("caseSource")
    void expectedSelectionAndFeasibilityAreIndependentlySpecified(Case test) {
        var decision = evaluate(test).result().decision();
        assertEquals(test.expectedRoute(), decision.selectedRoute(), test.description());
        assertEquals(new HashSet<>(test.feasibleRoutes()), new HashSet<>(decision.evaluations().stream()
            .filter(RouteEvaluation::feasible).map(RouteEvaluation::route).toList()));
        if (decision.selectedRoute() == null) assertTrue(test.feasibleRoutes().isEmpty());
        else assertTrue(test.feasibleRoutes().contains(decision.selectedRoute()));
    }
    @Test void exportsAllStrategiesAgainstIndependentFeasibilityLabels() throws Exception {
        List<String> csv = new ArrayList<>();
        csv.add("scenario,source,policy,expectedAdaptiveRoute,selectedRoute,feasible,expectedMatch,abstained,constraintViolation");
        var random = new Random(20261028);
        for (var test : cases()) {
            var selected = new LinkedHashMap<String, String>();
            selected.put("ALWAYS_EDGE", "EDGE"); selected.put("ALWAYS_CLOUD", "CLOUD");
            selected.put("RANDOM", List.of("EDGE", "CLOUD", "HYBRID").get(random.nextInt(3)));
            selected.put("ADAPTIVE", evaluate(test).result().decision().selectedRoute());
            for (var entry : selected.entrySet()) {
                boolean abstained = entry.getValue() == null;
                boolean feasible = test.feasibleRoutes().contains(entry.getValue());
                csv.add(String.join(",", test.scenario().name(), "SIMULATED", entry.getKey(),
                    String.valueOf(test.expectedRoute()), String.valueOf(entry.getValue()), String.valueOf(feasible),
                    String.valueOf(Objects.equals(test.expectedRoute(), entry.getValue())), String.valueOf(abstained),
                    String.valueOf(!abstained && !feasible)));
            }
        }
        Files.createDirectories(Path.of("target/research"));
        Files.write(Path.of("target/research/expanded-policy-comparison.csv"), csv);
    }
}
