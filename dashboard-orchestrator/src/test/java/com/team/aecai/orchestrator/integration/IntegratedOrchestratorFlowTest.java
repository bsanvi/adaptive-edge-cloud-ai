package com.team.aecai.orchestrator.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Local, deterministic HTTP integration tests. Uses four independent fake services;
 * DOES NOT claim the real five-process system or a second laptop has been tested.
 */
class IntegratedOrchestratorFlowTest {
    private final List<HttpServer> servers = new ArrayList<>();
    private final AtomicInteger edgePredict = new AtomicInteger();
    private final AtomicInteger edgePreprocess = new AtomicInteger();
    private final AtomicInteger edgePostprocess = new AtomicInteger();
    private final AtomicInteger cloudPredict = new AtomicInteger();
    private final AtomicInteger cloudHybrid = new AtomicInteger();
    private final AtomicReference<String> decisionInput = new AtomicReference<>("");
    private final AtomicReference<String> route = new AtomicReference<>("edge");
    private final AtomicReference<String> decisionStatus = new AtomicReference<>("selected");
    private boolean failCloudPredict;
    private boolean rejectCloudInput;
    private boolean failEdgePredict;
    private boolean failDecision;
    private boolean failEstimator;
    private boolean cloudReady = true;
    private IntegratedOrchestrator orchestrator;
    private IntegratedHistory history;

    @BeforeEach
    void setUp() throws IOException {
        HttpServer edge = server();
        edge.createContext("/edge/metrics", e -> json(e,200,
                "{\"cpu_percent\":30.0,\"ram_percent\":40.0,\"battery_percent\":75.0,\"power_plugged\":true}"));
        edge.createContext("/api/v1/edge/anomaly/ready", e -> json(e,200,"{\"status\":\"READY\"}"));
        edge.createContext("/api/v1/edge/anomaly/predict", e -> {
            edgePredict.incrementAndGet();
            if (failEdgePredict) json(e,503,"{\"error\":\"simulated edge outage\"}");
            else json(e,200,"{\"prediction\":\"NORMAL\",\"confidence\":0.8}");
        });
        edge.createContext("/api/v1/edge/anomaly/preprocess", e -> {
            edgePreprocess.incrementAndGet(); json(e,200,
                    "{\"featureSchemaVersion\":\"features-v1\",\"features\":[0,0,0,0,0,0,0,0]}");
        });
        edge.createContext("/api/v1/edge/anomaly/postprocess", e -> {
            edgePostprocess.incrementAndGet(); json(e,200,"{\"prediction\":\"NORMAL\",\"confidence\":0.8}");
        });

        HttpServer cloud = server();
        cloud.createContext("/api/v1/cloud/ready", e -> json(e, cloudReady ? 200 : 503,
                cloudReady ? "{\"status\":\"READY\"}" : "{\"status\":\"NOT_READY\"}"));
        cloud.createContext("/api/v1/cloud/probe", e -> {
            byte[] bytes = new byte[65536];
            java.util.Arrays.fill(bytes,(byte)'x');
            e.getResponseHeaders().set("Content-Type","application/octet-stream");
            e.sendResponseHeaders(200,bytes.length);
            try (var output=e.getResponseBody()){output.write(bytes);}
        });
        cloud.createContext("/api/v1/cloud/predict", e -> {
            cloudPredict.incrementAndGet();
            if(rejectCloudInput) {json(e,400,"{\"error\":\"invalid input\"}");return;}
            if(failCloudPredict) json(e,503,"{\"error\":\"simulated cloud outage\"}");
            else json(e,200,"{\"prediction\":\"NORMAL\",\"confidence\":0.8}");
        });
        cloud.createContext("/api/v1/cloud/hybrid/infer", e -> {
            cloudHybrid.incrementAndGet();
            json(e,200,"{\"classProbabilities\":{\"NORMAL\":0.8,\"ANOMALY\":0.2}}");
        });

        HttpServer estimator = server();
        estimator.createContext("/api/v1/estimate/routes", e -> {
            if (failEstimator) {json(e,503,"{\"error\":\"simulated estimator outage\"}");return;}
            json(e,200,"""
                {"estimates":{
                  "EDGE":{"estimateAvailable":true,"estimatedLatencyMs":10.0,"energyEstimateJ":0.2,"carbonEstimateG":0.001,"communicationMb":0.0},
                  "CLOUD":{"estimateAvailable":true,"estimatedLatencyMs":15.0,"energyEstimateJ":0.3,"carbonEstimateG":0.002,"communicationMb":0.1},
                  "HYBRID":{"estimateAvailable":true,"estimatedLatencyMs":13.0,"energyEstimateJ":0.25,"carbonEstimateG":0.0015,"communicationMb":0.05}
                }}
                """);
        });

        HttpServer decision = server();
        decision.createContext("/decision/evaluate", e -> {
            decisionInput.set(new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            if (failDecision) {json(e,503,"{\"error\":\"simulated decision outage\"}");return;}
            if("no_feasible_route".equals(decisionStatus.get())) {
                json(e,200,"{\"status\":\"no_feasible_route\",\"selected_location\":null,\"explanation\":\"No feasible route\"}");
            } else {
                json(e,200,"{\"status\":\"selected\",\"selected_location\":\""+route.get()+"\",\"explanation\":\"Mock decision\"}");
            }
        });

        for(HttpServer s:servers) s.start();
        history = mock(IntegratedHistory.class);
        orchestrator = new IntegratedOrchestrator(history, url(edge),url(cloud),url(estimator),url(decision),
                0.02,500,400);
    }

    @AfterEach
    void tearDown() { for(HttpServer s:servers) s.stop(0); }

    @Test
    void privacyRestrictedTaskRunsOnlyOnEdge() {
        route.set("edge");
        var r=orchestrator.run(task(true));
        assertEquals("SUCCESS",r.status());
        assertEquals("EDGE",r.selectedRoute());
        assertEquals("EDGE",r.executedRoute());
        assertEquals("NORMAL",r.prediction());
        assertTrue(decisionInput.get().contains("\"privacy_required\":true"));
        assertEquals(1,edgePredict.get());
        assertEquals(0,cloudPredict.get());
        assertEquals(0,cloudHybrid.get());
        verify(history).save(r);
    }

    @Test
    void remoteCloudRouteActuallyRunsCloudPredict() {
        route.set("cloud");
        var r=orchestrator.run(task(false));
        assertEquals("SUCCESS",r.status());
        assertEquals("CLOUD",r.selectedRoute());
        assertEquals("CLOUD",r.executedRoute());
        assertFalse(r.fallbackUsed());
        assertEquals(1,cloudPredict.get());
        assertEquals(0,edgePredict.get());
        verify(history).save(r);
    }

    @Test
    void hybridExecutesPreprocessCloudInferAndPostprocess() {
        route.set("hybrid");
        var r=orchestrator.run(task(false));
        assertEquals("SUCCESS",r.status(),r.error());
        assertEquals("HYBRID",r.selectedRoute());
        assertEquals("HYBRID",r.executedRoute());
        assertEquals(1,edgePreprocess.get());
        assertEquals(1,cloudHybrid.get());
        assertEquals(1,edgePostprocess.get());
        assertEquals(0,cloudPredict.get());
        verify(history).save(r);
    }

    @Test
    void remoteFailureFallsBackToEdgeWhenDeadlineAllows() {
        route.set("cloud");
        failCloudPredict=true;
        var r=orchestrator.run(task(false));
        assertEquals("SUCCESS",r.status(),r.error());
        assertEquals("CLOUD",r.selectedRoute());
        assertEquals("EDGE",r.executedRoute());
        assertTrue(r.fallbackUsed());
        assertEquals(1,cloudPredict.get());
        assertEquals(1,edgePredict.get());
        verify(history).save(r);
    }

    @Test
    void noFeasibleRouteNeverExecutesInference() {
        decisionStatus.set("no_feasible_route");
        var r=orchestrator.run(task(false));
        assertEquals("NO_FEASIBLE_ROUTE",r.status());
        assertNull(r.executedRoute());
        assertEquals(0,edgePredict.get());
        assertEquals(0,cloudPredict.get());
        assertEquals(0,cloudHybrid.get());
        verify(history).save(r);
    }

    @Test
    void refusesPrivacyViolatingDecisionBeforeAnyInference() {
        route.set("cloud");
        var r=orchestrator.run(task(true));
        assertEquals("FAILED",r.status());
        assertTrue(r.error().contains("Privacy constraint violation"));
        assertEquals(0,cloudPredict.get());
        assertEquals(0,cloudHybrid.get());
        assertEquals(0,edgePredict.get());
        verify(history).save(r);
    }

    @Test
    void unavailableDecisionEngineFailsWithoutExecutingAnyModel() {
        failDecision = true;
        var r = orchestrator.run(task(false));
        assertEquals("FAILED", r.status());
        assertNull(r.executedRoute());
        assertEquals(0, edgePredict.get());
        assertEquals(0, cloudPredict.get());
        assertEquals(0, cloudHybrid.get());
        verify(history).save(r);
    }

    @Test
    void unavailableEstimatorFailsBeforeDecisionOrInference() {
        failEstimator = true;
        var r = orchestrator.run(task(false));
        assertEquals("FAILED", r.status());
        assertNull(r.selectedRoute());
        assertNull(r.executedRoute());
        assertEquals(0, edgePredict.get());
        assertEquals(0, cloudPredict.get());
        verify(history).save(r);
    }

    @Test
    void selectedEdgeFailureIsNotMisreportedAsSuccess() {
        route.set("edge");
        failEdgePredict = true;
        var r = orchestrator.run(task(false));
        assertEquals("FAILED", r.status());
        assertEquals("EDGE", r.selectedRoute());
        assertNull(r.executedRoute());
        assertFalse(r.fallbackUsed());
        verify(history).save(r);
    }

    @Test
    void cloudFailureWithInsufficientDeadlineDoesNotClaimFallback() {
        route.set("cloud");
        failCloudPredict = true;
        var tight = new IntegratedOrchestrator.RunTask("tight", task(false).samples(),
                0.001, false, "LOW", null);
        var r = orchestrator.run(tight);
        assertEquals("FAILED", r.status());
        assertEquals("CLOUD", r.selectedRoute());
        assertNull(r.executedRoute());
        assertFalse(r.fallbackUsed());
        assertEquals(0, edgePredict.get());
        verify(history).save(r);
    }

    @Test
    void analysisDoesNotInferAndSavedDecisionExecutesOnlyOnce() {
        route.set("hybrid");
        var analysis = orchestrator.analyze(task(false));
        assertEquals("ANALYZED", analysis.report().status());
        assertEquals(0, edgePreprocess.get());
        assertEquals(0, cloudHybrid.get());
        verify(history).saveAnalysis(analysis);
        verify(history, never()).save(any());
        route.set("edge"); // execution must use the saved decision, not re-select
        var result = orchestrator.executeAnalysis(analysis.analysisId());
        assertEquals("HYBRID", result.executedRoute());
        assertEquals(1, cloudHybrid.get());
        assertThrows(IllegalStateException.class, () -> orchestrator.executeAnalysis(analysis.analysisId()));
        assertEquals(1, cloudHybrid.get());
    }

    @Test
    void analysisAuditContainsDeviceButNoRawSamples() {
        var analysis = orchestrator.analyze(task(true));
        var json = tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(analysis);
        assertTrue(json.contains("cpu_percent"));
        assertTrue(json.contains("decisionRequest"));
        assertFalse(json.contains("\"samples\""));
    }

    @Test
    void failedAnalysisCannotExecute() {
        failDecision = true;
        var analysis = orchestrator.analyze(task(false));
        assertEquals("FAILED", analysis.report().status());
        assertThrows(IllegalStateException.class, () -> orchestrator.executeAnalysis(analysis.analysisId()));
        assertEquals(0, edgePredict.get());
    }

    @Test
    void cloudClientErrorDoesNotMaskContractFailureWithFallback() {
        route.set("cloud");
        rejectCloudInput = true;
        var result = orchestrator.run(task(false));
        assertEquals("FAILED", result.status());
        assertEquals(1, cloudPredict.get());
        assertEquals(0, edgePredict.get());
        assertFalse(result.fallbackUsed());
    }

    private IntegratedOrchestrator.RunTask task(boolean privacy) {
        return new IntegratedOrchestrator.RunTask("person4-check",
                List.of(0.1,0.2,0.3,0.2,0.1,0.4,0.1,0.3,0.2,0.1,0.2,0.3),
                20000.0,privacy,"LOW",null);
    }

    private HttpServer server() throws IOException {
        HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        servers.add(s); return s;
    }
    private String url(HttpServer s) {return "http://127.0.0.1:"+s.getAddress().getPort();}
    private static void json(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type","application/json");
        exchange.sendResponseHeaders(status,bytes.length);
        try(var output=exchange.getResponseBody()) {output.write(bytes);}
    }
}
