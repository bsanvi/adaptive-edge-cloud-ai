package com.edgecloud.edge.anomaly;

import ai.onnxruntime.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/** Uses the SAME versioned model/feature schema as Person 3's cloud service. */
@Component
public class EdgeAnomalyModel {
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private OrtSession session;
    private final String version;
    private String checksum;

    public EdgeAnomalyModel(ResourceLoader loader,
            @Value("${edge.model-location:classpath:model/reference.onnx}") String location,
            @Value("${edge.model-sha256:}") String expected,
            @Value("${edge.model-version:reference-v1}") String version) {
        this.version = version;
        try {
            byte[] bytes;
            try (var input = loader.getResource(location).getInputStream()) {
                bytes = input.readAllBytes();
            }
            checksum = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
            if (expected.isBlank() && location.equals("classpath:model/reference.onnx")) {
                try (var input = loader.getResource("classpath:model/reference.sha256").getInputStream()) {
                    expected = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
                }
            }
            if (expected.isBlank() || !checksum.equalsIgnoreCase(expected)) {
                throw new IllegalArgumentException("Edge model checksum mismatch");
            }
            try (var opts = new OrtSession.SessionOptions()) {
                session = environment.createSession(bytes, opts);
            }
            var metadata = session.getMetadata().getCustomMetadata();
            if (!version.equals(metadata.get("modelVersion"))
                    || !"features-v1".equals(metadata.get("featureSchemaVersion"))
                    || !"NORMAL,ANOMALY".equals(metadata.get("labels"))) {
                throw new IllegalArgumentException("Unexpected ONNX model metadata");
            }
            if (!session.getInputNames().equals(java.util.Set.of("features"))
                    || !session.getOutputNames().contains("probabilities")) {
                throw new IllegalArgumentException("Unexpected ONNX tensor contract");
            }
            infer(new float[8]);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error(
                    "Edge model could not be loaded; Edge anomaly inference unavailable", ex);
            if (session != null) {
                try { session.close(); } catch (OrtException ignored) {}
                session = null;
            }
        }
    }

    public boolean ready() { return session != null; }
    public String version() { return version; }
    public String checksum() { return checksum; }

    public float[] infer(float[] features) {
        if (session == null) throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Edge anomaly model not ready");
        if (features == null || features.length != 8) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Exactly 8 features required");
        for (float value : features) {
            if (!Float.isFinite(value) || Math.abs(value) > 2_000_000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feature value");
            }
        }
        try (var tensor = OnnxTensor.createTensor(environment,
                FloatBuffer.wrap(features), new long[]{1, 8});
             var result = session.run(Map.of("features", tensor))) {
            Object output = result.get("probabilities").orElseThrow().getValue();
            if (!(output instanceof float[][] probs)
                    || probs.length != 1 || probs[0].length != 2) {
                throw new IllegalStateException("Expected [1,2] probabilities");
            }
            float[] p = probs[0].clone();
            if (!Float.isFinite(p[0]) || !Float.isFinite(p[1])
                    || p[0] < 0 || p[1] < 0 || p[0] > 1 || p[1] > 1
                    || Math.abs(p[0] + p[1] - 1) > 1e-5) {
                throw new IllegalStateException("Invalid probability values");
            }
            return p;
        } catch (OrtException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Edge ONNX inference failed", ex);
        }
    }

    @PreDestroy
    public void close() throws OrtException {
        if (session != null) session.close();
    }
}
