package com.adaptive.decision;
import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DecisionEndpointsTests {
    @LocalServerPort int port;
    @MockitoBean DecisionInputsProvider provider;
    @MockitoBean EstimationGateway gateway;
    final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    HttpResponse<String> post(String path, Object body) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/decision/"+path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    @Test void detailedAndBasicPreservePersonFourContractAndWeights() throws Exception {
        var scenario = WhatIfTests.scenario("normal", "PUBLIC", true, 1000, 100, "COST_PRIORITY");
        when(provider.resolve(any())).thenReturn(new DecisionSupportingInputs(
            new EstimatorInputs(128, 128, true, true, 500, 400, Instant.now(), "SIMULATED", "SIMULATED"),
            scenario.costs(), scenario.communicationCosts(), scenario.switchingCosts()));
        when(gateway.estimate(any(), any())).thenReturn(scenario.estimates());
        var basic = post("evaluate", scenario.request());
        var detailed = post("evaluate/detailed", scenario.request());
        assertEquals(200, basic.statusCode());
        assertEquals(200, detailed.statusCode());
        var basicJson = mapper.readTree(basic.body());
        var detailJson = mapper.readTree(detailed.body());
        assertEquals(Set.of("taskId", "status", "selectedRoute", "decisionTimeMs", "evaluations", "reasons"),
            new HashSet<>(basicJson.propertyNames()));
        assertEquals(basicJson.path("selectedRoute"), detailJson.path("decision").path("selectedRoute"));
        assertEquals(.6, detailJson.path("details").get(0).path("scoring").path("weights").path("cost").asDouble());
        assertEquals(3, detailJson.path("details").size());
    }
    @Test void whatIfUsesNoLiveIntegrationsAndRejectsInvalidInput() throws Exception {
        clearInvocations(provider, gateway);
        var baseline = WhatIfTests.scenario("base", "PUBLIC", true, 1000, 100, "BALANCED");
        var scenario = WhatIfTests.scenario("private", "PRIVATE", true, 1000, 100, "BALANCED");
        var response = post("what-if", new WhatIfRequest(baseline, List.of(scenario)));
        assertEquals(200, response.statusCode());
        var json = mapper.readTree(response.body());
        assertEquals("SIMULATED", json.path("source").asText());
        assertFalse(json.path("tasksExecuted").asBoolean());
        assertTrue(json.path("comparisons").get(0).path("routeChanged").asBoolean());
        assertEquals(400, post("what-if", new WhatIfRequest(null, List.of())).statusCode());
        verifyNoInteractions(provider, gateway);
    }
}
