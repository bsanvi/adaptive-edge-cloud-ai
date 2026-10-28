package com.adaptive.cloud.research;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;
import static com.adaptive.cloud.research.Research.*;

@RestController
@RequestMapping("/api/v2/cloud")
public class ResearchCloudController {
    private final ImageModel model;
    private final com.sun.management.OperatingSystemMXBean os =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    public ResearchCloudController(ImageModel model) { this.model = model; }
    @GetMapping("/ready") public Capability ready() { return model.capability(); }
    @GetMapping("/telemetry") public Device telemetry() {
        double cpu = os.getCpuLoad() * 100;
        long total = os.getTotalMemorySize(), free = os.getFreeMemorySize();
        return new Device(cpu >= 0 && cpu <= 100 ? cpu : null, total > 0 ? 100d * (total - free) / total : null,
                null, null, Instant.now(), "LIVE_MEASUREMENT", "OperatingSystemMXBean system CPU and physical RAM; power unavailable", model.environmentId);
    }
    private void remote(ImageInput input) {
        require(input != null && input.privacy() != null, "Privacy required");
        if (input.privacy() == Privacy.CRITICAL) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PRIVACY_CRITICAL");
        // Defense in depth only: orchestrator must reject before transmitting bytes.
    }
    @PostMapping("/predict") public Prediction predict(@RequestBody ImageInput input) { remote(input); return model.predict(input); }
    @PostMapping("/hybrid/infer") public Logits hybrid(@RequestBody ImageInput input) { remote(input); return model.logits(input, true); }
    @GetMapping(value="/probe", produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> probe(@RequestParam(defaultValue="262144") int bytes) {
        require(bytes >= 1 && bytes <= 1048576, "Probe range is 1..1048576 bytes");
        byte[] data = new byte[bytes]; java.util.Arrays.fill(data, (byte) 'x');
        return ResponseEntity.ok().header("Cache-Control", "no-store, no-transform").contentLength(bytes).body(data);
    }
    @PostMapping(value="/probe", consumes=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Map<String,Object> upload(@RequestBody byte[] data) {
        require(data.length >= 1 && data.length <= 1048576, "Probe range is 1..1048576 bytes");
        return Map.of("receivedBytes", data.length, "sampledAt", Instant.now());
    }
}
