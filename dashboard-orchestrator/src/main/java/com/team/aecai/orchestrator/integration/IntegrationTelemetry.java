package com.team.aecai.orchestrator.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only live telemetry. No synthetic measurements and no background jobs.
 * Each HTTP request samples the actual Edge metrics endpoint and service health.
 * Network measurements are explicitly historical, from the last executed task.
 */
@Service
@Profile("integration")
public class IntegrationTelemetry {
    private final IntegrationServiceStatus serviceStatus;
    private final IntegratedHistory history;
    private final JsonMapper mapper;
    private final String edgeUrl;

    public IntegrationTelemetry(
            IntegrationServiceStatus serviceStatus,
            IntegratedHistory history,
            JsonMapper mapper,
            @Value("${integration.edge-url:http://127.0.0.1:8081}") String edgeUrl) {
        this.serviceStatus = serviceStatus;
        this.history = history;
        this.mapper = mapper;
        this.edgeUrl = edgeUrl.replaceAll("/+$", "");
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sampledAt", Instant.now().toString());
        result.put("source", "LIVE_BACKEND_POLL");
        result.put("services", serviceStatus.statuses());

        Map<String, Object> device = readEdgeMetrics();
        result.put("device", device);
        result.put("deviceAvailable", device != null);

        // An H2 report contains measurements from the last completed task;
        // these must never be passed off as an ongoing live network probe.
        Map<String, Object> lastNetwork = null;
        Map<String, Object> lastRun = null;
        try {
            List<Map<String, Object>> recent = history.recent(1);
            if (!recent.isEmpty()) {
                Map<String, Object> row = recent.get(0);
                lastRun = new LinkedHashMap<>();
                lastRun.put("id", row.get("ID"));
                lastRun.put("taskId", row.get("TASK_ID"));
                lastRun.put("status", row.get("STATUS"));
                lastRun.put("selectedRoute", row.get("SELECTED_ROUTE"));
                lastRun.put("executedRoute", row.get("EXECUTED_ROUTE"));
                lastRun.put("totalTimeMs", row.get("TOTAL_TIME_MS"));
                lastRun.put("createdAt", String.valueOf(row.get("CREATED_AT")));
                Object id = row.get("ID");
                if (id instanceof Number number) {
                    Map<String, Object> report = history.report(number.longValue());
                    if (report != null && report.get("network") instanceof Map<?, ?> network) {
                        lastNetwork = new LinkedHashMap<>();
                        lastNetwork.put("latencyMs", finiteNumber(network.get("latencyMs")));
                        lastNetwork.put("bandwidthMbps", finiteNumber(network.get("bandwidthMbps")));
                        lastNetwork.put("connected", network.get("connected"));
                        lastNetwork.put("source", "LAST_EXECUTION_APPROXIMATE_PROBE");
                        lastNetwork.put("taskId", row.get("TASK_ID"));
                        lastNetwork.put("observedAt", String.valueOf(row.get("CREATED_AT")));
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Telemetry must still work when H2 has old rows without a report.
        }
        result.put("lastRun", lastRun);
        result.put("lastNetwork", lastNetwork);
        return result;
    }

    private Map<String, Object> readEdgeMetrics() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(edgeUrl + "/edge/metrics")
                    .toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1400);
            connection.setInstanceFollowRedirects(false);
            if (connection.getResponseCode() != 200) return null;
            byte[] bytes;
            try (var stream = connection.getInputStream()) {
                bytes = stream.readNBytes(32_769);
            }
            if (bytes.length > 32_768) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = mapper.readValue(
                    new String(bytes, StandardCharsets.UTF_8), Map.class);
            Object cpu = finiteNumber(raw.get("cpu_percent"));
            Object ram = finiteNumber(raw.get("ram_percent"));
            if (cpu == null || ram == null) return null;
            Map<String, Object> device = new LinkedHashMap<>();
            device.put("cpuPercent", cpu);
            device.put("ramPercent", ram);
            device.put("ramUsedMb", finiteNumber(raw.get("ram_used_mb")));
            device.put("ramAvailableMb", finiteNumber(raw.get("ram_available_mb")));
            device.put("batteryPercent", finiteNumber(raw.get("battery_percent")));
            device.put("powerPlugged", raw.get("power_plugged") instanceof Boolean
                    ? raw.get("power_plugged") : null);
            device.put("edgeTimestamp", raw.get("timestamp"));
            device.put("source", "MEASURED_EDGE_METRICS");
            return device;
        } catch (Exception ignored) {
            return null; // Missing data is shown as unavailable, never invented.
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static Double finiteNumber(Object value) {
        if (value instanceof Number number) {
            double result = number.doubleValue();
            return Double.isFinite(result) ? result : null;
        }
        return null;
    }
}
