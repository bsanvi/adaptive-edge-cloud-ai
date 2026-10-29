package com.team.aecai.orchestrator.integration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntegrationTelemetryTest {
    private HttpServer server;

    private String edgeServer(String body, int code) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/edge/metrics", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() { if (server != null) server.stop(0); }

    @Test
    void readsRealEdgeMetricsAndKeepsOldNetworkProbeExplicitlyHistorical() throws Exception {
        String url = edgeServer("""
                {"cpu_percent":24.5,"ram_percent":61.2,
                 "battery_percent":80.0,"power_plugged":true}
                """, 200);
        var health = mock(IntegrationServiceStatus.class);
        when(health.statuses()).thenReturn(Map.of("Dashboard", true, "Edge", true,
                "Cloud", true, "Decision", true, "Estimator", true));
        var history = mock(IntegratedHistory.class);
        when(history.recent(1)).thenReturn(List.of(Map.of(
                "ID", 12L, "TASK_ID", "real-12", "STATUS", "SUCCESS",
                "SELECTED_ROUTE", "CLOUD", "EXECUTED_ROUTE", "CLOUD",
                "TOTAL_TIME_MS", 210.0, "CREATED_AT", "2026-10-27 22:15:00")));
        when(history.report(12L)).thenReturn(Map.of("network", Map.of(
                "connected", true, "latencyMs", 12.4, "bandwidthMbps", 30.5)));
        var telemetry = new IntegrationTelemetry(health, history, JsonMapper.builder().build(), url);
        Map<String, Object> result = telemetry.snapshot();
        assertEquals(true, result.get("deviceAvailable"));
        assertEquals("LIVE_BACKEND_POLL", result.get("source"));
        assertNotNull(result.get("sampledAt"));
        @SuppressWarnings("unchecked") Map<String, Object> device =
                (Map<String, Object>) result.get("device");
        assertEquals(24.5, (Double) device.get("cpuPercent"), 0.001);
        assertEquals(61.2, (Double) device.get("ramPercent"), 0.001);
        assertEquals(80.0, (Double) device.get("batteryPercent"), 0.001);
        assertEquals("MEASURED_EDGE_METRICS", device.get("source"));
        @SuppressWarnings("unchecked") Map<String, Object> network =
                (Map<String, Object>) result.get("lastNetwork");
        assertEquals("LAST_EXECUTION_APPROXIMATE_PROBE", network.get("source"));
        assertEquals("real-12", network.get("taskId"));
        assertEquals(12.4, (Double) network.get("latencyMs"), 0.001);
    }

    @Test
    void missingEdgeNeverProducesSyntheticOrCachedCpuData() throws Exception {
        String url = edgeServer("{\"error\":\"offline\"}", 503);
        var health = mock(IntegrationServiceStatus.class);
        when(health.statuses()).thenReturn(Map.of("Dashboard",true,"Edge",false));
        var history = mock(IntegratedHistory.class);
        when(history.recent(1)).thenReturn(List.of());
        var telemetry = new IntegrationTelemetry(health, history, JsonMapper.builder().build(), url);
        Map<String, Object> result = telemetry.snapshot();
        assertEquals(false, result.get("deviceAvailable"));
        assertNull(result.get("device"));
        assertNull(result.get("lastNetwork"));
        assertNull(result.get("lastRun"));
    }

    @Test
    void malformedMetricsOrMissingCpuAreMarkedUnavailable() throws Exception {
        String url = edgeServer("{\"ram_percent\":60}", 200);
        var health = mock(IntegrationServiceStatus.class);
        when(health.statuses()).thenReturn(Map.of("Dashboard",true));
        var history = mock(IntegratedHistory.class);
        when(history.recent(1)).thenReturn(List.of());
        var telemetry = new IntegrationTelemetry(health, history, JsonMapper.builder().build(), url);
        assertNull(telemetry.snapshot().get("device"));
    }
}
