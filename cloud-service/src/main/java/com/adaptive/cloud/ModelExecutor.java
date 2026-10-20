package com.adaptive.cloud;

import ai.onnxruntime.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import java.nio.FloatBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Component
public class ModelExecutor {
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private OrtSession session;
    private final String version;
    private String checksum;
    private String failure;

    public ModelExecutor(ResourceLoader loader, @Value("${cloud.model-location}") String location,
                         @Value("${cloud.model-sha256:}") String expected,
                         @Value("${cloud.model-version}") String version) {
        this.version = version;
        try {
            byte[] bytes;
            try (var input = loader.getResource(location).getInputStream()) { bytes = input.readAllBytes(); }
            checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (expected.isBlank() && location.equals("classpath:model/reference.onnx")) {
                try (var input = loader.getResource("classpath:model/reference.sha256").getInputStream()) {
                    expected = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                }
            }
            if (expected.isBlank() || !checksum.equalsIgnoreCase(expected)) throw new IllegalArgumentException("Model checksum mismatch");
            try (var options = new OrtSession.SessionOptions()) { session = environment.createSession(bytes, options); }
            var metadata=session.getMetadata().getCustomMetadata();
            if (!version.equals(metadata.get("modelVersion")) || !"features-v1".equals(metadata.get("featureSchemaVersion"))
                || !"NORMAL,ANOMALY".equals(metadata.get("labels")))
                throw new IllegalArgumentException("Model metadata version, feature schema or labels mismatch");
            if (!session.getInputNames().equals(java.util.Set.of("features")) || !session.getOutputNames().contains("probabilities"))
                throw new IllegalArgumentException("Unsupported model input/output names");
            infer(new float[8]); // Validate input/output shape and probabilities before marking ready.
        } catch (Exception e) {
            failure = "Model unavailable or incompatible; verify artifact, SHA-256 and tensor contract";
            if (session != null) { try { session.close(); } catch (OrtException ignored) {} session = null; }
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Cloud model initialization failed", e);
        }
    }

    public boolean ready() { return session != null && failure == null; }
    public String version() { return version; }
    public String checksum() { return checksum; }

    public float[] infer(float[] features) {
        if (session == null) throw new ApiFailure(503, "SERVICE_UNAVAILABLE", "Model is not ready");
        if (features == null || features.length != 8) throw new ApiFailure(400,"VALIDATION_ERROR","Exactly eight features required");
        for (float x : features) if (!Float.isFinite(x) || Math.abs(x)>2_000_000)
            throw new ApiFailure(400,"OUT_OF_RANGE","Features must be finite and within +/-2000000");
        try (var tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(features), new long[]{1,8});
             var result = session.run(Map.of("features", tensor))) {
            Object output = result.get("probabilities").orElseThrow().getValue();
            if (!(output instanceof float[][] values) || values.length != 1 || values[0].length != 2)
                throw new IllegalStateException("Expected float32 probabilities [1,2]");
            float[] p = values[0].clone();
            if (!Float.isFinite(p[0]) || !Float.isFinite(p[1]) || p[0]<0 || p[1]<0 || p[0]>1 || p[1]>1 || Math.abs(p[0]+p[1]-1)>1e-5)
                throw new IllegalStateException("Invalid model probabilities");
            return p;
        } catch (OrtException | IllegalStateException e) {
            throw new ApiFailure(502, "INFERENCE_FAILURE", "Model execution failed");
        }
    }

    @PreDestroy public void close() throws OrtException { if (session != null) session.close(); }
}
