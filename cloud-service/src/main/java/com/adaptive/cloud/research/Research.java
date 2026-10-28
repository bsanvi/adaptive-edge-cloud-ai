package com.adaptive.cloud.research;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** HTTP-only v2 contract. Generated into each independently built service. */
public final class Research {
    private Research() {}
    public enum Route { EDGE, CLOUD, HYBRID }
    public enum Privacy { NORMAL, CRITICAL }
    public enum Priority { BALANCED, PERFORMANCE_FIRST, ENERGY_SAVER, SUSTAINABILITY_FIRST, LOW_COMMUNICATION_COST, CUSTOM }
    public enum Strategy { ALWAYS_EDGE, ALWAYS_CLOUD, ALWAYS_HYBRID, RANDOM, EXISTING_ADAPTIVE, UNCALIBRATED_ADAPTIVE, CALIBRATED_ADAPTIVE }
    public record ImageInput(String requestId, String imageBase64, String modelVersion, Privacy privacy) {}
    public record Profile(long fileSizeBytes, int width, int height, long pixelCount, String format,
                          String complexity, String complexityMethod, String modelVersion,
                          List<Integer> modelInputShape, String preprocessingVersion, long hybridImageBytes) {}
    public record ImageProfile(Profile profile, double profilingMs) {}
    public record Stages(double preprocessingMs, double inferenceMs, double postprocessingMs, double remotePreprocessingMs) {
        public Stages(double pre, double inference, double post) { this(pre, inference, post, 0); }
        public double total() { return preprocessingMs + inferenceMs + postprocessingMs + remotePreprocessingMs; }
    }
    public record ClassScore(int classIndex, String label, double probability) {}
    public record Prediction(String requestId, String modelVersion, String modelSha256, String labelsSha256,
                             int classIndex, String label, double confidence, List<ClassScore> top5, Stages stages) {}
    public record Prepared(String requestId, String modelVersion, String preprocessingVersion,
                           String imageBase64, long originalBytes, long preparedBytes, double preprocessingMs) {}
    public record Logits(String requestId, String modelVersion, String modelSha256, String labelsSha256,
                         float[] logits, Stages stages) {}
    public record Device(Double cpuPercent, Double ramPercent, Double batteryPercent, Boolean powerPlugged,
                         Instant sampledAt, String source, String method, String environmentId) {}
    public record Network(boolean connected, Double rttMs, Double uploadMbps, Double downloadMbps,
                          Instant sampledAt, String source, String method, long probeBytes) {}
    public record Capability(boolean ready, String modelVersion, String modelSha256, String labelsSha256,
                             String preprocessingVersion, String environmentId, Map<String,Object> hardware) {}
    public record EstimateInput(String requestId, Profile profile, Device device, Network network,
                                boolean edgeCapable, boolean cloudCapable, String edgeEnvironment,
                                String cloudEnvironment, long rawRequestBytes, long hybridRequestBytes,
                                long resultBytes, String estimatorVersion) {}
    public record Estimate(boolean available, String reason, Double totalMs, Stages stages,
                           Double transferMs, Double overheadMs, Double energyJ, Double deviceEnergyJ,
                           Double carbonG, Long uploadBytes, Long downloadBytes, Double communicationMb,
                           String source, int observations, String calibrationKey) {}
    public record Estimates(String requestId, String estimatorVersion, Map<Route,Estimate> routes,
                            Map<String,Object> assumptions) {}
    public record Observation(String observationId, EstimateInput context, Route route, Stages stages,
                              double executionMs, long uploadBytes, long downloadBytes, boolean successful,
                              String source, Instant measuredAt) {}
    public record DecisionInput(String requestId, Privacy privacy, double remainingDeadlineMs,
                                Priority priority, Map<String,Double> customWeights, Device device,
                                Network network, Estimates estimates, Route previousRoute, boolean legacyScoring) {}
    public record Evaluation(Route route, boolean feasible, List<String> reasons, Double score,
                             Map<String,Double> normalized, Map<String,Double> contributions) {}
    public record Decision(String requestId, String status, Route selectedRoute, List<Evaluation> evaluations,
                           Map<String,Double> weights, Map<String,Double> normalizationCaps,
                           double switchingThreshold, String explanation, String policyVersion,
                           String estimatorVersion, double decisionMs) {}
    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    public static boolean finite(Double value) { return value != null && Double.isFinite(value) && value >= 0; }
}
