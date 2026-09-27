package com.adaptive.cloud;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cloud")
public class CloudController {
    public enum Privacy { NORMAL, CRITICAL }
    public record RawRequest(@NotBlank @Size(max=100) String requestId, @NotBlank @Size(max=100) String taskId,
                             @NotNull Privacy privacyLevel, @NotNull @Size(min=8,max=4096) double[] samples) {}
    public record HybridRequest(@NotBlank @Size(max=100) String requestId, @NotBlank @Size(max=100) String taskId,
                                @NotNull Privacy privacyLevel, @NotBlank String featureSchemaVersion,
                                @NotNull @Size(min=8,max=8) float[] features) {}
    private final FeatureExtractor extractor;
    private final ModelExecutor model;
    private final int maxProbe;
    public CloudController(FeatureExtractor extractor, ModelExecutor model, @Value("${cloud.probe-max-bytes}") int maxProbe) {
        this.extractor=extractor; this.model=model; this.maxProbe=maxProbe;
    }
    @GetMapping("/health") public Map<String,Object> health() { return Map.of("status","UP","service","cloud-service"); }
    @GetMapping("/ready") public ResponseEntity<?> ready() {
        return ResponseEntity.status(model.ready()?200:503).body(Map.of("status",model.ready()?"READY":"NOT_READY",
            "modelVersion",model.version(),"modelSha256",model.checksum()==null?"":model.checksum()));
    }
    @GetMapping("/probe") public ResponseEntity<byte[]> probe(@RequestParam(defaultValue="65536") int bytes) {
        if (bytes<1 || bytes>maxProbe) throw new ApiFailure(400,"OUT_OF_RANGE","Probe bytes must be between 1 and "+maxProbe);
        byte[] payload = new byte[bytes];
        java.util.Arrays.fill(payload, (byte)'x');
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(bytes)
            .header("X-Server-Time",Instant.now().toString()).header("Cache-Control","no-store, no-transform").body(payload);
    }
    private void permit(String id, Privacy privacy, HttpServletRequest servlet) {
        servlet.setAttribute("requestId",id);
        if (privacy==Privacy.CRITICAL) throw new ApiFailure(403,"PRIVACY_CRITICAL","Remote inference is forbidden for CRITICAL tasks");
    }
    @PostMapping("/predict") public Map<String,Object> predict(@Valid @RequestBody RawRequest request, HttpServletRequest servlet) {
        permit(request.requestId(),request.privacyLevel(),servlet);
        long start=System.nanoTime();
        float[] p=model.infer(extractor.extract(request.samples()));
        boolean anomaly=p[1]>=0.5f;
        return Map.of("requestId",request.requestId(),"prediction",anomaly?"ANOMALY":"NORMAL",
            "confidence",anomaly?p[1]:p[0],"modelVersion",model.version(),"inferenceTimeMs",(System.nanoTime()-start)/1e6);
    }
    @PostMapping("/hybrid/infer") public Map<String,Object> hybrid(@Valid @RequestBody HybridRequest request, HttpServletRequest servlet) {
        permit(request.requestId(),request.privacyLevel(),servlet);
        if (!"features-v1".equals(request.featureSchemaVersion())) throw new ApiFailure(409,"CONTRACT_VERSION_MISMATCH","Expected features-v1");
        long start=System.nanoTime();
        float[] p=model.infer(request.features());
        return Map.of("requestId",request.requestId(),"modelVersion",model.version(),
            "classProbabilities",Map.of("NORMAL",p[0],"ANOMALY",p[1]),"inferenceTimeMs",(System.nanoTime()-start)/1e6);
    }
}
