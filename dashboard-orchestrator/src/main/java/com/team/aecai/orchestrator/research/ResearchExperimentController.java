package com.team.aecai.orchestrator.research;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Decision-only what-if experiments. Simulated inputs are passed to the REAL
 * Person 3 estimator and Person 2 Python Decision Engine, without any
 * inference execution or synthetic scoring in JavaScript or this controller.
 * Never exposes mandatory privacy/deadline rules as mutable settings.
 */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/experiments")
public class ResearchExperimentController {
    private final RestClient estimator;
    private final RestClient decision;
    private final double usdPerMb;

    public ResearchExperimentController(
            @Value("${integration.estimator-url:http://127.0.0.1:8084}") String estimatorUrl,
            @Value("${integration.decision-url:http://127.0.0.1:8001}") String decisionUrl,
            @Value("${integration.assumed-usd-per-mb:0.02}") double usdPerMb) {
        this.estimator = client(estimatorUrl);
        this.decision = client(decisionUrl);
        this.usdPerMb = usdPerMb;
    }

    private static RestClient client(String base) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(12000);
        return RestClient.builder().baseUrl(base).requestFactory(factory).build();
    }

    public record Scenario(String taskId, Double cpuPercent, Double ramPercent,
                           Double batteryPercent, Double latencyMs, Double bandwidthMbps,
                           Double deadlineMs, Long inputSizeBytes,
                           Boolean edgeCapable, Boolean cloudCapable,
                           Boolean privacyRequired, Double carbonGPerKwh) {}

    @PostMapping(value="/evaluate", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> evaluate(@RequestBody Scenario s) {
        try {
            validate(s);
            var taskId = s.taskId() == null || s.taskId().isBlank() ? "scenario-" + UUID.randomUUID() : s.taskId();
            boolean networkAvailable = s.cloudCapable() && s.bandwidthMbps() > 0;
            boolean connected = networkAvailable;
            var complexity = s.inputSizeBytes() < 100_000 ? "LOW" : s.inputSizeBytes() < 2_000_000 ? "MEDIUM" : "HIGH";
            var traceId = UUID.randomUUID().toString();

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("requestId", traceId);
            request.put("task", Map.of("taskId", taskId, "taskType", "SENSOR_ANOMALY",
                    "complexity", complexity, "dataSizeBytes", s.inputSizeBytes(),
                    "estimatedFeatureBytes", 32, "estimatedResultBytes", 128));
            Map<String,Object> device = new LinkedHashMap<>();
            device.put("cpuPercent", s.cpuPercent());
            device.put("ramPercent", s.ramPercent());
            device.put("batteryPercent", s.batteryPercent());
            device.put("source", "SIMULATED");
            request.put("device", device);
            Map<String,Object> network = new LinkedHashMap<>();
            network.put("connected", connected);
            network.put("bandwidthMbps", connected ? s.bandwidthMbps() : null);
            network.put("networkLatencyMs", connected ? s.latencyMs() : null);
            network.put("sampledAt", Instant.now().toString());
            network.put("source", "SIMULATED");
            request.put("network", network);
            request.put("edgeCapable", s.edgeCapable());
            request.put("cloudCapable", s.cloudCapable());
            request.put("remainingDeadlineMs", s.deadlineMs());
            request.put("edgeCarbonIntensityGPerKwh", 500.0); // explicit prototype assumption
            request.put("cloudCarbonIntensityGPerKwh", s.carbonGPerKwh());

            Map<String,Object> estimation = post(estimator, "/api/v1/estimate/routes", request);
            Map<String,Object> estimates = map(estimation.get("estimates"));
            Map<String,Object> decisionEstimates = new LinkedHashMap<>();
            for (String r : List.of("EDGE","CLOUD","HYBRID")) {
                Map<String,Object> e = map(estimates.get(r));
                if (!Boolean.TRUE.equals(e.get("estimateAvailable"))) continue;
                double communicationMb = dbl(e.get("communicationMb"));
                decisionEstimates.put(r.toLowerCase(Locale.ROOT), Map.of(
                        "estimated_time_ms", dbl(e.get("estimatedLatencyMs")),
                        "estimated_energy_j", dbl(e.get("energyEstimateJ")),
                        "estimated_carbon_g", dbl(e.get("carbonEstimateG")),
                        "estimated_cost", communicationMb * usdPerMb));
            }
            Map<String,Object> pythonDevice = new LinkedHashMap<>();
            pythonDevice.put("cpu_percent",s.cpuPercent());
            pythonDevice.put("ram_percent",s.ramPercent());
            pythonDevice.put("battery_percent",s.batteryPercent());
            pythonDevice.put("power_plugged",false); // scenario assumption, not measured
            Map<String,Object> pythonRequest = new LinkedHashMap<>();
            pythonRequest.put("task", Map.of("task_id",taskId,"task_type","sensor_anomaly",
                    "data_size_mb",s.inputSizeBytes() / 1_000_000.0,
                    "complexity", complexity.equals("LOW") ? 0.25 : complexity.equals("MEDIUM") ? 0.5 : 0.85,
                    "deadline_ms",s.deadlineMs(),"privacy_required",s.privacyRequired()));
            pythonRequest.put("edge",pythonDevice);
            pythonRequest.put("network",Map.of("latency_ms",connected?s.latencyMs():0.0,
                    "bandwidth_mbps",connected?s.bandwidthMbps():0.0));
            pythonRequest.put("estimates", decisionEstimates);
            pythonRequest.put("previous_location",null);
            Map<String,Object> decisionResult = post(decision,"/decision/evaluate",pythonRequest);
            return ResponseEntity.ok(Map.of("source","SIMULATED_INPUT_REAL_ENGINE",
                    "sampledAt", Instant.now().toString(),"decision",decisionResult,
                    "estimates",estimates,"device",device,"network",network));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));
        } catch (RestClientException ex) {
            return ResponseEntity.status(503).body(Map.of("error","Estimator or Decision Engine unavailable; no simulated result was substituted."));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(502).body(Map.of("error","Invalid upstream estimate or decision response"));
        }
    }

    static void validate(Scenario s) {
        if (s == null || s.taskId() == null || s.taskId().length() > 100 || s.taskId().isBlank()
                || s.edgeCapable() == null || s.cloudCapable() == null || s.privacyRequired() == null
                || s.inputSizeBytes() == null || s.inputSizeBytes() < 64 || s.inputSizeBytes() > 1_048_576
                || !range(s.cpuPercent(),0,100) || !range(s.ramPercent(),0,100)
                || !range(s.batteryPercent(),0,100) || !range(s.latencyMs(),0,5000)
                || !range(s.bandwidthMbps(),0,10000) || !range(s.deadlineMs(),0.001,10_000_000)
                || !range(s.carbonGPerKwh(),0,5000))
            throw new IllegalArgumentException("Invalid what-if parameters: 0-100% resources, positive deadline, 64-1,048,576 bytes, finite network/carbon values.");
    }
    private static boolean range(Double v,double min,double max){return v!=null&&Double.isFinite(v)&&v>=min&&v<=max;}
    private static double dbl(Object v){if(v instanceof Number n&&Double.isFinite(n.doubleValue()))return n.doubleValue();throw new IllegalStateException("Missing estimate value");}
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object v){return v instanceof Map<?,?> m?(Map<String,Object>)m:Map.of();}
    @SuppressWarnings("unchecked") private static Map<String,Object> post(RestClient c,String path,Object payload){var result=c.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);if(result==null)throw new IllegalStateException("Empty upstream response");return result;}
}
