package com.adaptive.estimate.research;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import static com.adaptive.estimate.research.Research.*;

@Service
public class CalibratedEstimator {
    public record Bucket(int count, Stages stages, double overheadMs) {}
    public record Snapshot(String version, Instant createdAt, Map<String,Bucket> buckets, Set<String> observationIds) {}
    private static final String ASSUMED = "assumed-image-v2";
    private final Path directory;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private Snapshot latest = new Snapshot(ASSUMED, Instant.EPOCH, Map.of(), Set.of());
    private final double edgeWatts, cloudWatts, networkJPerMb, edgeCarbon, cloudCarbon, networkCarbon;
    public CalibratedEstimator(@Value("${research.calibration-directory:data/calibration}") String directory,
            @Value("${research.edge-watts:15}") double edgeWatts, @Value("${research.cloud-watts:35}") double cloudWatts,
            @Value("${research.network-j-per-mb:0.5}") double networkJPerMb,
            @Value("${research.edge-carbon-g-per-kwh:500}") double edgeCarbon,
            @Value("${research.cloud-carbon-g-per-kwh:500}") double cloudCarbon,
            @Value("${research.network-carbon-g-per-kwh:500}") double networkCarbon) throws IOException {
        this.directory = Path.of(directory); this.edgeWatts = edgeWatts; this.cloudWatts = cloudWatts;
        this.networkJPerMb = networkJPerMb; this.edgeCarbon = edgeCarbon; this.cloudCarbon = cloudCarbon; this.networkCarbon = networkCarbon;
        for (double value : new double[]{edgeWatts, cloudWatts, networkJPerMb, edgeCarbon, cloudCarbon, networkCarbon}) require(finite(value), "Invalid energy/carbon assumption");
        Files.createDirectories(this.directory);
        try (var files = Files.list(this.directory)) {
            var last = files.filter(p -> p.getFileName().toString().matches("ewma-v2-[0-9]{8}\\.json")).max(Comparator.comparing(Path::toString));
            if (last.isPresent()) latest = mapper.readValue(last.get().toFile(), Snapshot.class);
        }
    }
    public synchronized Snapshot snapshot() { return latest; }
    private synchronized Snapshot version(String version) {
        if (version == null || version.equals("LATEST")) return latest;
        if (version.equals(ASSUMED)) return new Snapshot(ASSUMED, Instant.EPOCH, Map.of(), Set.of());
        require(version.matches("ewma-v2-[0-9]{8}"), "Invalid estimator version");
        if (version.equals(latest.version())) return latest;
        try { return mapper.readValue(directory.resolve(version + ".json").toFile(), Snapshot.class); }
        catch (Exception e) { throw new IllegalArgumentException("Estimator version unavailable: " + version); }
    }
    public static String key(EstimateInput r, Route route) {
        long pixels = r.profile().pixelCount();
        String size = pixels <= 250000 ? "small" : pixels <= 2000000 ? "medium" : "large";
        Double cpu = r.device() == null ? null : r.device().cpuPercent();
        String load = cpu == null ? "unknown" : cpu < 33 ? "idle" : cpu < 66 ? "moderate" : "busy";
        return route + "|" + r.profile().modelVersion() + "|" + r.edgeEnvironment() + "|" + r.cloudEnvironment() + "|" + size + "|" + load;
    }
    private static boolean fresh(Instant timestamp) {
        if (timestamp == null) return false;
        long age = Duration.between(timestamp, Instant.now()).toMillis(); return age >= 0 && age <= 5000;
    }
    private static void validate(EstimateInput r) {
        require(r != null && r.profile() != null && r.requestId() != null && r.network() != null, "Profile and network context required");
        require(r.profile().pixelCount() > 0 && r.profile().fileSizeBytes() > 0 && r.profile().modelVersion() != null, "Invalid profile");
        require(r.rawRequestBytes() > 0 && r.hybridRequestBytes() > 0 && r.resultBytes() > 0
                && r.rawRequestBytes() <= 12 * 1024 * 1024 && r.hybridRequestBytes() <= 12 * 1024 * 1024 && r.resultBytes() <= 1048576, "Invalid serialized byte counts");
        if (r.device() != null) for (Double percent : new Double[]{r.device().cpuPercent(), r.device().ramPercent(), r.device().batteryPercent()})
            require(percent == null || finite(percent) && percent <= 100, "Invalid device percentage");
    }
    public Estimates estimate(EstimateInput r) {
        validate(r); Snapshot snapshot = version(r.estimatorVersion());
        var routes = new LinkedHashMap<Route,Estimate>();
        for (Route route : Route.values()) {
            String reason = null; Device device = r.device(); Network n = r.network();
            if (route != Route.CLOUD && !r.edgeCapable()) reason = "EDGE_UNAVAILABLE";
            else if (route != Route.CLOUD && (device == null || device.cpuPercent() == null || device.ramPercent() == null || !fresh(device.sampledAt()))) reason = "DEVICE_METRICS_UNAVAILABLE";
            else if (route != Route.EDGE && !r.cloudCapable()) reason = "CLOUD_UNAVAILABLE";
            else if (route != Route.EDGE && !n.connected()) reason = "NETWORK_UNAVAILABLE";
            else if (route != Route.EDGE && (!fresh(n.sampledAt()) || !finite(n.rttMs()) || !finite(n.uploadMbps()) || !finite(n.downloadMbps()) || n.uploadMbps() <= 0 || n.downloadMbps() <= 0)) reason = "NETWORK_METRICS_UNAVAILABLE_OR_STALE";
            if (reason != null) {
                routes.put(route, new Estimate(false, reason, null, null, null, null, null, null, null, null, null, null, "MODEL_ESTIMATE", 0, key(r, route))); continue;
            }
            String key = key(r, route); Bucket bucket = snapshot.buckets().get(key);
            boolean calibrated = bucket != null && bucket.count() >= 5;
            double slowdown = device == null || device.cpuPercent() == null ? 1 : 1 + device.cpuPercent() / 100;
            double pre = 2 + 4 * r.profile().pixelCount() / 1e6 + 2 * r.profile().fileSizeBytes() / 1e6;
            Stages stages = calibrated ? bucket.stages() : switch (route) {
                case EDGE -> new Stages(pre * slowdown, 30 * slowdown, 1);
                case CLOUD -> new Stages(pre, 15, 1);
                case HYBRID -> new Stages(pre * slowdown, 15, 1, 1);
            };
            long up = route == Route.EDGE ? 0 : route == Route.CLOUD ? r.rawRequestBytes() : r.hybridRequestBytes();
            // Hybrid returns 1000 logits; the conservative byte assumption is recorded.
            long down = route == Route.EDGE ? 0 : route == Route.HYBRID ? Math.max(24000, r.resultBytes()) : r.resultBytes();
            double transfer = route == Route.EDGE ? 0 : transfer(up, down, n);
            double overhead = calibrated ? bucket.overheadMs() : route == Route.HYBRID ? 6 : 2;
            double edgeMs = route == Route.EDGE ? stages.total() : route == Route.HYBRID ? stages.preprocessingMs() + stages.postprocessingMs() : 0;
            double remoteMs = route == Route.CLOUD ? stages.total() : route == Route.HYBRID ? stages.inferenceMs() + stages.remotePreprocessingMs() : 0;
            double localJ = edgeMs * edgeWatts / 1000, remoteJ = remoteMs * cloudWatts / 1000;
            double communication = (up + down) / 1e6, linkJ = communication * networkJPerMb;
            double carbon = (localJ * edgeCarbon + remoteJ * cloudCarbon + linkJ * networkCarbon) / 3_600_000;
            routes.put(route, new Estimate(true, null, stages.total() + transfer + overhead, stages, transfer, overhead,
                    localJ + remoteJ + linkJ, localJ + linkJ / 2, carbon, up, down, communication,
                    calibrated ? "CALIBRATED_EWMA" : "ASSUMED_COEFFICIENT", bucket == null ? 0 : bucket.count(), key));
        }
        Map<String,Object> assumptions = new LinkedHashMap<>();
        assumptions.put("energySource", "ASSUMED_COEFFICIENT"); assumptions.put("carbonSource", "ASSUMED_COEFFICIENT");
        assumptions.put("edgeWatts", edgeWatts); assumptions.put("cloudWatts", cloudWatts); assumptions.put("networkJPerMb", networkJPerMb);
        assumptions.put("edgeCarbonGPerKwh", edgeCarbon); assumptions.put("cloudCarbonGPerKwh", cloudCarbon); assumptions.put("networkCarbonGPerKwh", networkCarbon);
        assumptions.put("carbonIntensityTimestamp", "UNAVAILABLE: configured assumption, not a live API observation");
        assumptions.put("energyBoundary", "Compute phases plus whole-link byte model; orchestration/idle/cooling excluded. Half link allocated to device.");
        assumptions.put("transferBoundary", "UTF-8 JSON bytes; excludes headers/TLS/link overhead. One RTT plus separate upload/download transfer times.");
        assumptions.put("minimumObservations", 5); assumptions.put("ewmaAlpha", .2); assumptions.put("snapshotCreatedAt", snapshot.createdAt());
        assumptions.put("coldStart", "Preprocess=2+4*megapixels+2*fileMB ms; Edge infer=30*(1+CPU/100) ms; Cloud infer=15 ms; not complexity-based routing.");
        return new Estimates(r.requestId(), snapshot.version(), Collections.unmodifiableMap(routes), assumptions);
    }
    public static double transfer(long up, long down, Network network) {
        return network.rttMs() + 8d * up / (network.uploadMbps() * 1000) + 8d * down / (network.downloadMbps() * 1000);
    }
    public synchronized Snapshot observe(Observation observation) throws IOException {
        require(observation != null && observation.observationId() != null && observation.observationId().length() <= 100
                && observation.route() != null && observation.stages() != null, "Complete observation required");
        validate(observation.context());
        require(observation.successful() && "LIVE_MEASUREMENT".equals(observation.source()), "Only successful measured execution observations can calibrate");
        require(observation.measuredAt() != null && !observation.measuredAt().isAfter(Instant.now()), "Valid observation timestamp required");
        var s = observation.stages();
        for (double value : new double[]{s.preprocessingMs(), s.inferenceMs(), s.postprocessingMs(), s.remotePreprocessingMs(), observation.executionMs()}) require(finite(value), "Invalid measured timing");
        require(observation.executionMs() + .01 >= s.total() && observation.uploadBytes() >= 0 && observation.downloadBytes() >= 0, "Invalid timing/byte boundaries");
        require(observation.context().device() == null || "LIVE_MEASUREMENT".equals(observation.context().device().source()), "Simulated device context cannot train live calibration");
        require("LIVE_MEASUREMENT".equals(observation.context().network().source()), "Simulated network context cannot train live calibration");
        if (latest.observationIds().contains(observation.observationId())) return latest;
        String key = key(observation.context(), observation.route());
        double modeledTransfer = 0;
        if (observation.route() != Route.EDGE) {
            Network n = observation.context().network();
            require(finite(n.rttMs()) && finite(n.uploadMbps()) && finite(n.downloadMbps()) && n.uploadMbps() > 0 && n.downloadMbps() > 0, "Measured network required for remote observation");
            modeledTransfer = transfer(observation.uploadBytes(), observation.downloadBytes(), n);
        }
        double residual = Math.max(0, observation.executionMs() - s.total() - modeledTransfer);
        Bucket previous = latest.buckets().get(key); int count = previous == null ? 1 : previous.count() + 1;
        Stages average = previous == null ? s : new Stages(ewma(previous.stages().preprocessingMs(), s.preprocessingMs()),
                ewma(previous.stages().inferenceMs(), s.inferenceMs()), ewma(previous.stages().postprocessingMs(), s.postprocessingMs()),
                ewma(previous.stages().remotePreprocessingMs(), s.remotePreprocessingMs()));
        var buckets = new LinkedHashMap<>(latest.buckets());
        buckets.put(key, new Bucket(count, average, previous == null ? residual : ewma(previous.overheadMs(), residual)));
        var ids = new HashSet<>(latest.observationIds()); ids.add(observation.observationId());
        String version = "ewma-v2-%08d".formatted(ids.size());
        Snapshot next = new Snapshot(version, Instant.now(), Map.copyOf(buckets), Set.copyOf(ids));
        Path temporary = Files.createTempFile(directory, "snapshot-", ".tmp");
        try {
            mapper.writeValue(temporary.toFile(), next);
            Files.move(temporary, directory.resolve(version + ".json"), StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
        latest = next; return next;
    }
    private static double ewma(double previous, double actual) { return .8 * previous + .2 * actual; }
}
