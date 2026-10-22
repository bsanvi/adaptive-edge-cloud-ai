package com.edgecloud.edge.anomaly;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Adds a common anomaly workload WITHOUT removing Person 1's image-resize endpoints. */
@RestController
@RequestMapping("/api/v1/edge/anomaly")
public class EdgeAnomalyController {
    public record RawRequest(@NotBlank String requestId, @NotBlank String taskId,
            @NotNull @Size(min = 8, max = 4096) double[] samples) {}
    public record PostprocessRequest(@NotBlank String requestId,
            @NotBlank String taskId, @NotNull Map<String, Double> classProbabilities) {}

    private final EdgeAnomalyModel model;
    public EdgeAnomalyController(EdgeAnomalyModel model) { this.model = model; }

    @GetMapping("/ready")
    public ResponseEntity<?> ready() {
        return ResponseEntity.status(model.ready() ? 200 : 503).body(Map.of(
                "status", model.ready() ? "READY" : "NOT_READY",
                "modelVersion", model.version(),
                "featureSchemaVersion", "features-v1",
                "modelSha256", model.checksum() == null ? "" : model.checksum()));
    }

    @PostMapping("/predict")
    public Map<String, Object> predict(@Valid @RequestBody RawRequest request) {
        long start = System.nanoTime();
        float[] probs = model.infer(AnomalyFeatures.extract(request.samples()));
        return prediction(request.requestId(), request.taskId(), probs,
                (System.nanoTime() - start) / 1e6);
    }

    @PostMapping("/preprocess")
    public Map<String, Object> preprocess(@Valid @RequestBody RawRequest request) {
        return Map.of("requestId", request.requestId(), "taskId", request.taskId(),
                "featureSchemaVersion", "features-v1",
                "features", AnomalyFeatures.extract(request.samples()));
    }

    @PostMapping("/postprocess")
    public Map<String, Object> postprocess(@Valid @RequestBody PostprocessRequest request) {
        Double normal = request.classProbabilities().get("NORMAL");
        Double anomaly = request.classProbabilities().get("ANOMALY");
        if (normal == null || anomaly == null || !Double.isFinite(normal)
                || !Double.isFinite(anomaly) || normal < 0 || anomaly < 0
                || normal > 1 || anomaly > 1 || Math.abs(normal + anomaly - 1) > 1e-4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid class probabilities");
        }
        return prediction(request.requestId(), request.taskId(),
                new float[]{normal.floatValue(), anomaly.floatValue()}, 0);
    }

    private Map<String, Object> prediction(String requestId, String taskId,
            float[] probs, double inferenceMs) {
        boolean anomalous = probs[1] >= 0.5f;
        return Map.of("requestId", requestId,
                "taskId", taskId, "prediction", anomalous ? "ANOMALY" : "NORMAL",
                "confidence", anomalous ? probs[1] : probs[0],
                "modelVersion", model.version(), "inferenceTimeMs", inferenceMs);
    }
}
