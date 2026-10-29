package com.adaptive.decision;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;
import com.adaptive.decision.scoring.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchScenariosTests {
    @Test void expectedRoutesAndPolicyComparisons() throws Exception {
        var scenarios = List.of(
            WhatIfTests.scenario("normal-busy", "PUBLIC", true, 1000, 100, "BALANCED"),
            WhatIfTests.scenario("private", "PRIVATE", true, 1000, 100, "BALANCED"),
            WhatIfTests.scenario("critical", "CRITICAL", true, 1000, 100, "COST_PRIORITY"),
            WhatIfTests.scenario("offline", "PUBLIC", false, 1000, 100, "BALANCED"),
            WhatIfTests.scenario("deadline-cloud-only", "PUBLIC", true, 100, 100, "BALANCED"),
            WhatIfTests.scenario("impossible-deadline", "PUBLIC", true, 50, 100, "BALANCED"),
            WhatIfTests.scenario("device-ready", "PUBLIC", true, 1000, 0, "BALANCED"));
        String[] expected = {"CLOUD", "EDGE", "EDGE", "EDGE", "CLOUD", null, "EDGE"};
        var service = new WhatIfService(ScoringConfiguration.defaults());
        List<String> csv = new ArrayList<>();
        csv.add("scenario,source,policy,expectedAdaptiveRoute,selectedRoute,feasible,expectedMatch");
        var random = new Random(20261028);
        for (int i=0; i<scenarios.size(); i++) {
            var scenario = scenarios.get(i);
            var evaluation = service.evaluate(new WhatIfRequest(scenario, List.of(
                WhatIfTests.scenario("comparison", "PUBLIC", true, 1000, 100, "BALANCED")))).baseline();
            var decision = evaluation.result().decision();
            assertEquals(expected[i], decision.selectedRoute(), scenario.name());
            Map<String, String> selected = new LinkedHashMap<>();
            selected.put("ALWAYS_EDGE", "EDGE"); selected.put("ALWAYS_CLOUD", "CLOUD");
            selected.put("RANDOM", List.of("EDGE", "CLOUD", "HYBRID").get(random.nextInt(3)));
            selected.put("ADAPTIVE", decision.selectedRoute());
            for (var entry : selected.entrySet()) {
                boolean feasible = decision.evaluations().stream().anyMatch(r -> r.feasible() && Objects.equals(r.route(), entry.getValue()));
                csv.add(String.join(",", scenario.name(), "SIMULATED", entry.getKey(), String.valueOf(expected[i]),
                    String.valueOf(entry.getValue()), String.valueOf(feasible), String.valueOf(Objects.equals(expected[i], entry.getValue()))));
            }
        }
        Files.createDirectories(Path.of("target/research"));
        Files.write(Path.of("target/research/policy-comparison.csv"), csv);
    }
}
