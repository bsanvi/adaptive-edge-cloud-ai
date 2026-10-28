package com.adaptive.decision;

import com.adaptive.decision.dto.*;
import com.adaptive.decision.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** Real HTTP into decision-service and out to a synthetic Person 3 estimator. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MockTeamIntegrationTests {
    static final MockPeer estimator = new MockPeer("/api/v1/estimate/routes");
    static final MockPeer edge = new MockPeer("/api/v1/edge/metrics");
    static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();
    @LocalServerPort int port;
    @MockitoBean DecisionInputsProvider inputs;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("services.estimator.base-url", estimator::url);
        registry.add("services.estimator.connect-timeout-ms", () -> 300);
        registry.add("services.estimator.read-timeout-ms", () -> 1000);
    }
    @AfterAll static void stop() { estimator.close(); edge.close(); }
    @BeforeEach void setup() throws Exception {
        estimator.status = 200; estimator.delayMs = 0; estimator.calls.set(0);
        estimator.response = JSON.writeValueAsString(WhatIfTests.scenario("base", "PUBLIC", true, 1000, 100, "BALANCED").estimates());
        edge.response = """
            {"cpu_percent":100,"ram_percent":100,"ram_used_mb":8000,"ram_available_mb":0,
             "battery_percent":null,"power_plugged":null,"timestamp":"2026-10-28T00:00:00Z"}
            """;
        when(inputs.resolve(any())).thenAnswer(invocation -> new DecisionSupportingInputs(
            new EstimatorInputs(256, 128, true, true, 500, 400,
                Instant.now().minusMillis(100), "SIMULATED", "SIMULATED"),
            new RouteCostInputs(WhatIfTests.ZERO), WhatIfTests.ZERO, WhatIfTests.ZERO));
        clearInvocations(inputs);
    }
    // A test-only Orchestrator adapter translates Person 1 snake_case metrics to Person 4 camelCase.
    DecisionRequest orchestratorRequest() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var reply = client.send(HttpRequest.newBuilder(URI.create(edge.url()+"/api/v1/edge/metrics")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            var metrics = JSON.readTree(reply.body());
            var task = WhatIfTests.scenario("base", "PUBLIC", true, 1000, 100, "BALANCED").request().task();
            return new DecisionRequest(task, new DeviceMetrics(Instant.parse(metrics.path("timestamp").asText()),
                metrics.path("cpu_percent").asDouble(), metrics.path("ram_percent").asDouble(), 0, false),
                new NetworkMetrics(true, 20, 50), null);
        }
    }
    HttpResponse<String> post(String path, Object request) throws Exception {
        return postJson(path, JSON.writeValueAsString(request));
    }
    HttpResponse<String> postJson(String path, String json) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/decision/"+path))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    // Exact Person 4 basic response field names, copied as test-only consumer DTOs.
    record OrchestratorDecision(String taskId, String status, String selectedRoute, double decisionTimeMs,
        List<OrchestratorRoute> evaluations, List<String> reasons) {}
    record OrchestratorRoute(String route, boolean feasible, double estimatedLatencyMs,
        double estimatedEnergyJ, double estimatedCarbonG, double communicationCost,
        double switchingCost, double score, List<String> rejectionReasons) {}

    @Test void mockEdgeToEstimatorToOrchestratorContractRoundTrip() throws Exception {
        var response = post("evaluate", orchestratorRequest());
        assertEquals(200, response.statusCode(), response.body());
        var result = JSON.readValue(response.body(), OrchestratorDecision.class);
        assertEquals("CLOUD", result.selectedRoute());
        assertEquals(3, result.evaluations().size());
        assertEquals(100, result.evaluations().get(1).estimatedLatencyMs());
        assertEquals(40, result.evaluations().get(1).estimatedEnergyJ());
        var wire = JSON.readTree(estimator.body.get());
        assertEquals("research", wire.path("requestId").asText());
        assertEquals(1024, wire.path("task").path("dataSizeBytes").asLong());
        assertEquals(256, wire.path("task").path("estimatedFeatureBytes").asLong());
        assertEquals(20, wire.path("network").path("networkLatencyMs").asDouble());
        assertEquals(50, wire.path("network").path("bandwidthMbps").asDouble());
        assertEquals("SIMULATED", wire.path("device").path("source").asText());
        assertTrue(wire.path("device").path("batteryPercent").isNull());
        assertFalse(wire.has("samples"));
    }
    @Test void detailedResponseContainsTheActualProfileAndEstimatorCosts() throws Exception {
        var request = WhatIfTests.scenario("cost", "PUBLIC", true, 1000, 100, "COST_PRIORITY").request();
        var response = post("evaluate/detailed", request);
        assertEquals(200, response.statusCode(), response.body());
        var json = JSON.readTree(response.body());
        assertEquals(.6, json.path("details").get(1).path("scoring").path("weights").path("cost").asDouble());
        assertEquals(40, json.path("decision").path("evaluations").get(1).path("estimatedEnergyJ").asDouble());
    }
    @ParameterizedTest @ValueSource(ints = {400, 500, 503})
    void upstreamHttpErrorsAreControlled(int status) throws Exception {
        estimator.status = status;
        var response = post("evaluate", orchestratorRequest());
        assertEquals(503, response.statusCode());
        assertEquals("ESTIMATOR_ERROR", JSON.readTree(response.body()).path("status").asText());
    }
    @Test void malformedEstimatorResponseIsControlled() throws Exception {
        estimator.response = "{broken";
        assertEquals(503, post("evaluate", orchestratorRequest()).statusCode());
    }
    @Test void mismatchedCorrelationIdIsControlled() throws Exception {
        estimator.response = estimator.response.replace("research", "wrong-id");
        assertEquals(503, post("evaluate", orchestratorRequest()).statusCode());
    }
    @Test void emptyEstimatorResponseIsControlled() throws Exception {
        estimator.status = 204;
        assertEquals(503, post("evaluate", orchestratorRequest()).statusCode());
    }
    @Test void slowEstimatorTimesOutWithoutExecutingOrFallingBack() throws Exception {
        estimator.delayMs = 2500;
        var response = post("evaluate", orchestratorRequest());
        assertEquals(503, response.statusCode());
        assertEquals("ESTIMATOR_UNAVAILABLE", JSON.readTree(response.body()).path("status").asText());
    }
    @Test void offlineRequestUsesNullRemoteMeasurementsOnEstimatorWire() throws Exception {
        var scenario = WhatIfTests.scenario("offline", "PUBLIC", false, 1000, 100, "BALANCED");
        var response = post("evaluate", scenario.request());
        assertEquals(200, response.statusCode());
        assertEquals("EDGE", JSON.readTree(response.body()).path("selectedRoute").asText());
        var network = JSON.readTree(estimator.body.get()).path("network");
        assertTrue(network.path("bandwidthMbps").isNull());
        assertTrue(network.path("networkLatencyMs").isNull());
    }
    @Test void unavailableEstimatesKeepNullCostsAndExposeUpstreamReason() throws Exception {
        var estimates = new HashMap<>(WhatIfTests.scenario("base", "PUBLIC", true, 1000, 100, "BALANCED").estimates().estimates());
        estimates.put("CLOUD", new RouteEstimate(false, "STALE_PROBE", null,null,null,null,null,null,null,null,null,null,"ESTIMATED"));
        estimator.response = JSON.writeValueAsString(new EstimatorResponse("research", "mock-v1", "ASSUMED", estimates));
        var response = post("evaluate/detailed", orchestratorRequest());
        var json = JSON.readTree(response.body());
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(json.path("decision").path("evaluations").get(1).path("estimatedLatencyMs").isNull());
        assertTrue(json.path("details").get(1).path("rejectionReasons").toString().contains("ESTIMATOR_STALE_PROBE"));
        assertTrue(json.path("details").get(1).path("scoring").isNull());
    }
    @Test void invalidRequestIsRejectedBeforeSupportingInputsOrEstimator() throws Exception {
        String json = JSON.writeValueAsString(orchestratorRequest()).replace("BALANCED", "TYPO");
        assertEquals(400, postJson("evaluate", json).statusCode());
        verifyNoInteractions(inputs);
        assertEquals(0, estimator.calls.get());
    }
    @ParameterizedTest
    @ValueSource(strings = {"cpuPercent", "ramPercent", "batteryPercent", "batteryAvailable", "connected", "latencyMs", "bandwidthMbps", "deadlineMs", "inputDataSizeBytes"})
    void missingPrimitiveTelemetryCannotSilentlyBecomeZero(String field) throws Exception {
        var json = JSON.valueToTree(orchestratorRequest());
        objectFor(json, field).remove(field);
        assertEquals(400, postJson("evaluate", json.toString()).statusCode());
        verifyNoInteractions(inputs);
        assertEquals(0, estimator.calls.get());
    }

    @Test void currentPrimitiveOrchestratorConsumerCannotSafelyReadUnavailableCosts() throws Exception {
        var estimates = new HashMap<>(WhatIfTests.scenario("base", "PUBLIC", true, 1000, 100, "BALANCED").estimates().estimates());
        estimates.put("CLOUD", new RouteEstimate(false, "NETWORK_UNAVAILABLE", null,null,null,null,null,null,null,null,null,null,"ESTIMATED"));
        estimator.response = JSON.writeValueAsString(new EstimatorResponse("research", "mock-v1", "ASSUMED", estimates));
        var response = post("evaluate", orchestratorRequest());
        assertEquals(200, response.statusCode());
        var strictConsumer = JsonMapper.builder().findAndAddModules()
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
        assertThrows(tools.jackson.core.JacksonException.class,
            () -> strictConsumer.readValue(response.body(), OrchestratorDecision.class));
        assertTrue(JSON.readTree(response.body()).path("evaluations").get(1).path("estimatedLatencyMs").isNull());
    }

    @ParameterizedTest @ValueSource(strings = {"cpuPercent", "ramPercent", "batteryPercent", "batteryAvailable", "connected", "latencyMs", "bandwidthMbps", "deadlineMs", "inputDataSizeBytes"})
    void explicitNullTelemetryCannotSilentlyBecomeZero(String field) throws Exception {
        var json = JSON.valueToTree(orchestratorRequest());
        objectFor(json, field).putNull(field);
        assertEquals(400, postJson("evaluate", json.toString()).statusCode());
        verifyNoInteractions(inputs);
        assertEquals(0, estimator.calls.get());
    }

    private tools.jackson.databind.node.ObjectNode objectFor(tools.jackson.databind.JsonNode json, String field) {
        String group = Set.of("connected", "latencyMs", "bandwidthMbps").contains(field) ? "network"
            : Set.of("deadlineMs", "inputDataSizeBytes").contains(field) ? "task" : "device";
        return (tools.jackson.databind.node.ObjectNode) json.path(group);
    }
}
