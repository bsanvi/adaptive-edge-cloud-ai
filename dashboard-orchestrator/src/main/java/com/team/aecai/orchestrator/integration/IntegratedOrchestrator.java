package com.team.aecai.orchestrator.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.client.HttpClientErrorException;
import java.util.*;

/**
 * Real five-process workflow, isolated from the original Iris/demo endpoints:
 * Edge metrics -> Cloud probe -> Person 3 estimator -> Person 2 decision ->
 * selected EDGE/CLOUD/HYBRID ONNX inference -> H2 history.
 * All remote calls use HTTP/1.1 through SimpleClientHttpRequestFactory.
 */
@Service
@org.springframework.scheduling.annotation.EnableScheduling
@Profile("integration")
public class IntegratedOrchestrator {
    public record RunTask(String taskId, List<Double> samples, double deadlineMs,
            boolean privacyRequired, String complexity, String previousRoute) {}

    public record RunResult(String taskId, String status, String selectedRoute,
            String executedRoute, boolean fallbackUsed, String prediction,
            Double confidence, double totalTimeMs, String explanation,
            String error, Map<String, Object> decision,
            Map<String, Object> estimates, Map<String, Object> network, Long reportId) {
        public RunResult(String taskId, String status, String selectedRoute, String executedRoute,
                boolean fallbackUsed, String prediction, Double confidence, double totalTimeMs,
                String explanation, String error, Map<String,Object> decision, Map<String,Object> estimates,
                Map<String,Object> network) {
            this(taskId,status,selectedRoute,executedRoute,fallbackUsed,prediction,confidence,totalTimeMs,
                    explanation,error,decision,estimates,network,null);
        }
        RunResult withReportId(long id) {
            return new RunResult(taskId,status,selectedRoute,executedRoute,fallbackUsed,prediction,confidence,
                    totalTimeMs,explanation,error,decision,estimates,network,id);
        }
    }

    public record Analysis(String analysisId, Instant expiresAt, RunResult report) {}
    private record Pending(RunTask task, Analysis analysis, long preparedNanos) {}
    private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();
    private static final long REVIEW_TTL_NANOS = 60_000_000_000L;

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 30000)
    public void expirePending() {
        pending.entrySet().removeIf(e -> System.nanoTime() - e.getValue().preparedNanos() > REVIEW_TTL_NANOS);
    }

    public synchronized Analysis analyze(RunTask task) {
        expirePending();
        if (pending.size() >= 100) throw new IllegalStateException("Too many pending analyses; wait one minute");
        RunResult report = process(task, null, true);
        String id = UUID.randomUUID().toString();
        Analysis analysis = new Analysis(id, Instant.now().plusSeconds(60), report);
        history.saveAnalysis(analysis);
        if ("ANALYZED".equals(report.status())) pending.put(id, new Pending(task, analysis, System.nanoTime()));
        return analysis;
    }

    public RunResult executeAnalysis(String id) {
        Pending prepared = pending.remove(id); // single consumer, including concurrent requests
        if (prepared == null) throw new IllegalStateException("Analysis unavailable or already executed; analyze again");
        if (System.nanoTime() - prepared.preparedNanos() > REVIEW_TTL_NANOS)
            throw new IllegalStateException("Analysis expired; analyze again with fresh metrics");
        return process(prepared.task(), prepared.analysis(), false);
    }

    private final RestClient edge;
    private final RestClient cloud;
    private final RestClient estimator;
    private final RestClient decision;
    private final IntegratedHistory history;
    private final double assumedUsdPerMb;
    private final double edgeCarbonIntensity;
    private final double cloudCarbonIntensity;

    public IntegratedOrchestrator(IntegratedHistory history,
            @Value("${integration.edge-url:http://127.0.0.1:8081}") String edgeUrl,
            @Value("${integration.cloud-url:http://127.0.0.1:8083}") String cloudUrl,
            @Value("${integration.estimator-url:http://127.0.0.1:8084}") String estimatorUrl,
            @Value("${integration.decision-url:http://127.0.0.1:8001}") String decisionUrl,
            @Value("${integration.assumed-usd-per-mb:0.02}") double assumedUsdPerMb,
            @Value("${integration.edge-carbon-intensity-g-per-kwh:500}") double edgeCarbonIntensity,
            @Value("${integration.cloud-carbon-intensity-g-per-kwh:400}") double cloudCarbonIntensity) {
        this.history = history;
        this.edge = rest(edgeUrl);
        this.cloud = rest(cloudUrl);
        this.estimator = rest(estimatorUrl);
        this.decision = rest(decisionUrl);
        this.assumedUsdPerMb = assumedUsdPerMb;
        this.edgeCarbonIntensity = edgeCarbonIntensity;
        this.cloudCarbonIntensity = cloudCarbonIntensity;
    }

    private static RestClient rest(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(15000);
        return RestClient.builder().baseUrl(baseUrl)
                .requestFactory(factory).build();
    }

    public RunResult run(RunTask task) {
        return process(task, null, false);
    }

    private RunResult process(RunTask task, Analysis prepared, boolean analyzeOnly) {
        validate(task);
        final long started = System.nanoTime();
        final String requestId = UUID.randomUUID().toString();
        String selected = null, executed = null, label = null, explanation = "";
        Double confidence = null;
        boolean fallback = false;
        Map<String, Object> decisionResponse = Map.of();
        Map<String, Object> estimates = Map.of();
        Map<String, Object> network = Map.of();
        String status = "SUCCESS", error = null;
        double analysisMs = prepared == null ? 0 : prepared.report().totalTimeMs();
        Map<String, Object> audit = new LinkedHashMap<>();
        List<Map<String, Object>> attempts = new ArrayList<>();
        audit.put("acceptedAt", Instant.now().toString());
        audit.put("task", Map.of("taskId", task.taskId(), "sampleCount", task.samples().size(),
                "complexity", task.complexity(), "deadlineMs", task.deadlineMs(), "privacyRequired", task.privacyRequired()));
        audit.put("deadlineScope", "Analysis processing plus execution; user review time excluded; analysis expires after 60 seconds");

        try {
            if (prepared == null) {
            // Real device telemetry from Person 1's existing metrics endpoint.
            Map<String, Object> device = get(edge, "/edge/metrics");
            audit.put("device", device);
            boolean edgeCapable = isReady(edge, "/api/v1/edge/anomaly/ready");
            boolean cloudCapable = isReady(cloud, "/api/v1/cloud/ready");

            NetworkProbe probe = probeCloud(cloudCapable);
            network = new LinkedHashMap<>();
            network.put("connected", probe.connected());
            network.put("latencyMs", probe.latencyMs());
            network.put("bandwidthMbps", probe.bandwidthMbps());
            network.put("sampledAt", Instant.now().toString());
            network.put("source", probe.connected() ? "MEASURED_APPROXIMATE" : "UNAVAILABLE");

            Map<String, Object> estimatesRequest = new LinkedHashMap<>();
            estimatesRequest.put("requestId", requestId);
            estimatesRequest.put("task", Map.of(
                    "taskId", task.taskId(), "taskType", "SENSOR_ANOMALY",
                    "complexity", task.complexity().toUpperCase(Locale.ROOT),
                    "dataSizeBytes", serializedSize(Map.of("requestId", requestId, "taskId", task.taskId(), "privacyLevel", "NORMAL", "samples", task.samples())),
                    "estimatedFeatureBytes", serializedSize(Map.of("requestId", requestId, "taskId", task.taskId(), "privacyLevel", "NORMAL", "featureSchemaVersion", "features-v1", "features", List.of(0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0))),
                    "estimatedResultBytes", 128));
            Map<String, Object> estDevice = new LinkedHashMap<>();
            estDevice.put("cpuPercent", number(device.get("cpu_percent")));
            estDevice.put("ramPercent", number(device.get("ram_percent")));
            estDevice.put("batteryPercent", nullableNumber(device.get("battery_percent")));
            estDevice.put("source", "MEASURED");
            estimatesRequest.put("device", estDevice);
            Map<String, Object> estNetwork = new LinkedHashMap<>();
            estNetwork.put("connected", probe.connected());
            estNetwork.put("bandwidthMbps", probe.connected() ? probe.bandwidthMbps() : null);
            estNetwork.put("networkLatencyMs", probe.connected() ? probe.latencyMs() : null);
            estNetwork.put("sampledAt", probe.connected() ? Instant.now().toString() : null);
            estNetwork.put("source", "MEASURED");
            estimatesRequest.put("network", estNetwork);
            estimatesRequest.put("edgeCapable", edgeCapable);
            estimatesRequest.put("cloudCapable", cloudCapable && probe.connected());
            estimatesRequest.put("remainingDeadlineMs", Math.max(0.001, task.deadlineMs() - elapsed(started)));
            estimatesRequest.put("edgeCarbonIntensityGPerKwh", edgeCarbonIntensity);
            estimatesRequest.put("cloudCarbonIntensityGPerKwh", cloudCarbonIntensity);
            Map<String, Object> estimation = post(estimator,
                    "/api/v1/estimate/routes", estimatesRequest);
            estimates = nested(estimation, "estimates");
            audit.put("payloadSizeScope", "UTF-8 JSON raw request; feature and result sizes are representative estimates, excluding HTTP headers");
            audit.put("estimatorRequest", estimatesRequest);
            audit.put("estimatorResponse", estimation);

            Map<String, Object> pythonRequest = new LinkedHashMap<>();
            pythonRequest.put("task", Map.of(
                    "task_id", task.taskId(),
                    "task_type", "sensor_anomaly",
                    "data_size_mb", task.samples().size() * 8.0 / 1_000_000,
                    "complexity", complexityScore(task.complexity()),
                    "deadline_ms", Math.max(0.001, task.deadlineMs() - elapsed(started)),
                    "privacy_required", task.privacyRequired()));
            Map<String, Object> pDevice = new LinkedHashMap<>();
            pDevice.put("cpu_percent", number(device.get("cpu_percent")));
            pDevice.put("ram_percent", number(device.get("ram_percent")));
            pDevice.put("battery_percent", nullableNumber(device.get("battery_percent")));
            pDevice.put("power_plugged", device.get("power_plugged"));
            pythonRequest.put("edge", pDevice);
            pythonRequest.put("network", Map.of(
                    "latency_ms", probe.connected() ? probe.latencyMs() : 0.0,
                    "bandwidth_mbps", probe.connected() ? probe.bandwidthMbps() : 0.0));
            pythonRequest.put("estimates", decisionEstimates(estimates, assumedUsdPerMb));
            pythonRequest.put("previous_location", normalizedPrevious(task.previousRoute()));
            audit.put("decisionRequest", pythonRequest);
            long decisionStarted = System.nanoTime();
            decisionResponse = post(decision, "/decision/evaluate", pythonRequest);
            audit.put("decisionRoundTripMs", elapsed(decisionStarted));
            audit.put("decisionRecordedAt", Instant.now().toString());
            } else {
                decisionResponse = new LinkedHashMap<>(prepared.report().decision());
                estimates = prepared.report().estimates();
                network = prepared.report().network();
                audit.putAll(optionalMap(decisionResponse.get("orchestrationAudit")));
                audit.put("analysisId", prepared.analysisId());
            }
            explanation = String.valueOf(decisionResponse.getOrDefault("explanation", ""));

            if ("no_feasible_route".equals(decisionResponse.get("status"))) {
                status = "NO_FEASIBLE_ROUTE";
            } else if (!"selected".equals(decisionResponse.get("status"))) {
                throw new IllegalStateException("Invalid Decision Engine status");
            } else {
                Object route = decisionResponse.get("selected_location");
                if (!(route instanceof String routeName)) {
                    throw new IllegalStateException("Decision Engine returned no selected route");
                }
                selected = routeName.toUpperCase(Locale.ROOT);
                if (!Set.of("EDGE", "CLOUD", "HYBRID").contains(selected)) {
                    throw new IllegalStateException("Unknown route: " + routeName);
                }
                // Defense in depth: never send sensitive raw data off device.
                if (task.privacyRequired() && !"EDGE".equals(selected)) {
                    throw new IllegalStateException("Privacy constraint violation");
                }
                if (!isAvailable(estimates, selected)) {
                    throw new IllegalStateException("Selected route has no valid estimate");
                }
                if (analyzeOnly) {
                    status = "ANALYZED";
                } else {
                if (prepared != null) {
                    if (("EDGE".equals(selected) || "HYBRID".equals(selected)) && !isReady(edge, "/api/v1/edge/anomaly/ready"))
                        throw new IllegalStateException("Edge is no longer ready; analyze again");
                    if (!"EDGE".equals(selected) && !isReady(cloud, "/api/v1/cloud/ready"))
                        throw new IllegalStateException("Cloud is no longer ready; analyze again");
                }
                double remaining = task.deadlineMs() - analysisMs - elapsed(started);
                if (number(optionalMap(estimates.get(selected)).get("estimatedLatencyMs")) > remaining)
                    throw new IllegalStateException("Insufficient remaining deadline before execution");
                try {
                    Map<String, Object> inference = infer(
                            selected, task, requestId, attempts);
                    label = String.valueOf(inference.get("prediction"));
                    confidence = nullableNumber(inference.get("confidence"));
                    executed = selected;
                } catch (RestClientException remoteFailure) {
                    if (!(remoteFailure instanceof HttpClientErrorException) && !"EDGE".equals(selected)
                            && !rejectedByDecision(decisionResponse, "edge")
                            && canFallbackToEdge(estimates, task.deadlineMs(), analysisMs + elapsed(started))) {
                        Map<String, Object> inference = infer("EDGE", task, requestId, attempts);
                        label = String.valueOf(inference.get("prediction"));
                        confidence = nullableNumber(inference.get("confidence"));
                        executed = "EDGE";
                        fallback = true;
                        explanation += " | Remote execution failed; fallback to Edge.";
                    } else {
                        throw remoteFailure;
                    }
                }
                }
            }
        } catch (RuntimeException ex) {
            status = "FAILED";
            error = ex instanceof org.springframework.web.client.RestClientResponseException response
                    ? "Upstream HTTP " + response.getStatusCode().value()
                    : ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        audit.put("attempts", attempts);
        audit.put("processingTimeMs", analysisMs + elapsed(started));
        audit.put("deadlineMet", "SUCCESS".equals(status) ? analysisMs + elapsed(started) <= task.deadlineMs() : null);
        decisionResponse = new LinkedHashMap<>(decisionResponse);
        decisionResponse.put("orchestrationAudit", audit);
        RunResult result = new RunResult(task.taskId(), status, selected, executed,
                fallback, label, confidence, analysisMs + elapsed(started), explanation, error,
                decisionResponse, estimates, network);
        if (!analyzeOnly) {
            long id = history.save(result);
            if (id > 0) result = result.withReportId(id);
        }
        return result;
    }

    private Map<String, Object> infer(String route, RunTask task, String requestId, List<Map<String,Object>> attempts) {
        long start = System.nanoTime();
        Map<String,Object> attempt = new LinkedHashMap<>();
        attempt.put("route", route);
        attempt.put("startedAt", Instant.now().toString());
        attempts.add(attempt);
        try {
            Map<String,Object> result = inferRoute(route, task, requestId);
            if (!(result.get("prediction") instanceof String label) || label.isBlank())
                throw new IllegalStateException("Inference returned no prediction");
            attempt.put("status", "SUCCESS");
            return result;
        } catch (RuntimeException ex) {
            attempt.put("status", "FAILED");
            attempt.put("errorType", ex.getClass().getSimpleName());
            throw ex;
        } finally { attempt.put("elapsedMs", elapsed(start)); }
    }

    private Map<String, Object> inferRoute(String route, RunTask task, String requestId) {
        Map<String, Object> input = Map.of("requestId", requestId,
                "taskId", task.taskId(), "samples", task.samples());
        return switch (route) {
            case "EDGE" -> post(edge, "/api/v1/edge/anomaly/predict", input);
            case "CLOUD" -> post(cloud, "/api/v1/cloud/predict", Map.of(
                    "requestId", requestId, "taskId", task.taskId(),
                    "privacyLevel", "NORMAL", "samples", task.samples()));
            case "HYBRID" -> {
                Map<String, Object> prepared = post(edge,
                        "/api/v1/edge/anomaly/preprocess", input);
                Object features = prepared.get("features");
                if (!(features instanceof List<?> arr) || arr.size() != 8
                        || !"features-v1".equals(prepared.get("featureSchemaVersion"))) {
                    throw new IllegalStateException("Invalid Edge features-v1 result");
                }
                Map<String, Object> remote = post(cloud,
                        "/api/v1/cloud/hybrid/infer", Map.of(
                                "requestId", requestId, "taskId", task.taskId(),
                                "privacyLevel", "NORMAL",
                                "featureSchemaVersion", "features-v1",
                                "features", features));
                yield post(edge, "/api/v1/edge/anomaly/postprocess", Map.of(
                        "requestId", requestId, "taskId", task.taskId(),
                        "classProbabilities", nested(remote, "classProbabilities")));
            }
            default -> throw new IllegalArgumentException("Unrecognized route");
        };
    }

    /** Only modelled route estimates are passed to Person 2; missing routes stay missing. */
    static Map<String, Object> decisionEstimates(Map<String, Object> routes, double pricePerMb) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String route : List.of("EDGE", "CLOUD", "HYBRID")) {
            Map<String, Object> data = optionalMap(routes.get(route));
            if (!Boolean.TRUE.equals(data.get("estimateAvailable"))) continue;
            double transmissionMb = number(data.get("communicationMb"));
            result.put(route.toLowerCase(Locale.ROOT), Map.of(
                    "estimated_time_ms", number(data.get("estimatedLatencyMs")),
                    "estimated_energy_j", number(data.get("energyEstimateJ")),
                    "estimated_carbon_g", number(data.get("carbonEstimateG")),
                    "estimated_cost", transmissionMb * pricePerMb));
        }
        return result;
    }

    static boolean isAvailable(Map<String, Object> routes, String route) {
        return Boolean.TRUE.equals(optionalMap(routes.get(route)).get("estimateAvailable"));
    }

    static double complexityScore(String complexity) {
        return switch (complexity.toUpperCase(Locale.ROOT)) {
            case "LOW" -> 0.25;
            case "MEDIUM" -> 0.50;
            case "HIGH" -> 0.85;
            default -> throw new IllegalArgumentException("Complexity must be LOW/MEDIUM/HIGH");
        };
    }

    static void validate(RunTask task) {
        if (task == null || task.taskId() == null || task.taskId().isBlank()
                || task.taskId().length() > 100 || task.samples() == null
                || task.samples().size() < 8 || task.samples().size() > 4096
                || !Double.isFinite(task.deadlineMs()) || task.deadlineMs() <= 0
                || task.deadlineMs() > 10_000_000 || task.complexity() == null) {
            throw new IllegalArgumentException("Invalid task: require ID, 8-4096 samples, positive deadline and complexity");
        }
        complexityScore(task.complexity());
        normalizedPrevious(task.previousRoute());
        for (Double d : task.samples()) {
            if (d == null || !Double.isFinite(d) || Math.abs(d) > 1_000_000) {
                throw new IllegalArgumentException("Samples must be finite and within +/-1000000");
            }
        }
    }

    private static String normalizedPrevious(String route) {
        if (route == null || route.isBlank()) return null;
        String normalized = route.toLowerCase(Locale.ROOT);
        if (!Set.of("edge", "cloud", "hybrid").contains(normalized)) {
            throw new IllegalArgumentException("Invalid previousRoute");
        }
        return normalized;
    }

    private static boolean rejectedByDecision(Map<String,Object> decision, String route) {
        Object rejection = optionalMap(decision.get("rejected_locations")).get(route);
        if (rejection instanceof Collection<?> reasons) return !reasons.isEmpty();
        if (rejection instanceof Map<?,?> reasons) return !reasons.isEmpty();
        return rejection != null && !rejection.toString().isBlank();
    }

    private boolean canFallbackToEdge(Map<String, Object> routes, double deadlineMs, double elapsedMs) {
        if (!isAvailable(routes, "EDGE")) return false;
        double estimatedEdgeMs = number(optionalMap(routes.get("EDGE")).get("estimatedLatencyMs"));
        return estimatedEdgeMs + elapsedMs <= deadlineMs;
    }

    private record NetworkProbe(boolean connected, double latencyMs, double bandwidthMbps) {}
    private NetworkProbe probeCloud(boolean cloudCapable) {
        if (!cloudCapable) return new NetworkProbe(false, 0, 0);
        try {
            long start = System.nanoTime();
            byte[] data = cloud.get().uri("/api/v1/cloud/probe?bytes=65536")
                    .retrieve().body(byte[].class);
            double durationMs = elapsed(start);
            if (data == null || data.length != 65536) return new NetworkProbe(false, 0, 0);
            // Approximate effective application throughput (includes RTT/server overhead).
            double mbps = data.length * 8.0 / (Math.max(durationMs, 0.1) * 1000);
            return new NetworkProbe(true, durationMs, mbps);
        } catch (RestClientException ex) {
            return new NetworkProbe(false, 0, 0);
        }
    }

    private static boolean isReady(RestClient client, String path) {
        try { return "READY".equals(get(client, path).get("status")); }
        catch (RestClientException ex) { return false; }
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> get(RestClient client, String path) {
        Map<String, Object> result = client.get().uri(path).retrieve().body(Map.class);
        if (result == null) throw new IllegalStateException("Empty response at " + path);
        return result;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> post(RestClient client, String path, Object body) {
        Map<String, Object> result = client.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(Map.class);
        if (result == null) throw new IllegalStateException("Empty response at " + path);
        return result;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> optionalMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }
    private static Map<String, Object> nested(Map<String, Object> value, String key) {
        Map<String, Object> result = optionalMap(value.get(key));
        if (result.isEmpty()) throw new IllegalStateException("Missing " + key);
        return result;
    }
    private static Double nullableNumber(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }
    private static double number(Object value) {
        if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue())) {
            throw new IllegalStateException("Missing or invalid numeric field: " + value);
        }
        return n.doubleValue();
    }
    private static int serializedSize(Object value) {
        return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsBytes(value).length;
    }
    private static double elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000.0;
    }
}
