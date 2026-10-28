package com.adaptive.decision.scoring;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Immutable configuration using the Python engine's JSON names and units. */
public record ScoringConfiguration(Map<String, Bounds> bounds,
                                   Map<String, Map<String, Double>> profiles,
                                   double readinessStrength, double switchingImprovement,
                                   double maxNetworkLatencyMs, double minBandwidthMbps) {
    public ScoringConfiguration(Map<String, Bounds> bounds, Map<String, Map<String, Double>> profiles) {
        this(bounds, profiles, defaults().readinessStrength(), defaults().switchingImprovement(),
             defaults().maxNetworkLatencyMs(), defaults().minBandwidthMbps());
    }
    public record Bounds(double min, double max) {
        public Bounds {
            if (!Double.isFinite(min) || !Double.isFinite(max) || min < 0 || max <= min)
                throw new IllegalArgumentException("Invalid normalization bounds");
        }
    }
    private static final Set<String> COMPONENTS = Set.of("time", "energy", "cost", "carbon");
    public ScoringConfiguration {
        if (bounds == null || !bounds.keySet().equals(COMPONENTS))
            throw new IllegalArgumentException("All normalization bounds are required");
        bounds = Map.copyOf(bounds);
        Map<String, Map<String, Double>> copy = new HashMap<>();
        if (profiles == null || !Double.isFinite(readinessStrength) || readinessStrength < 0 || readinessStrength > 1
                || !Double.isFinite(switchingImprovement) || switchingImprovement < 0 || switchingImprovement > 1
                || !Double.isFinite(maxNetworkLatencyMs) || maxNetworkLatencyMs < 0
                || !Double.isFinite(minBandwidthMbps) || minBandwidthMbps <= 0)
            throw new IllegalArgumentException("Invalid decision policy parameters");
        profiles.forEach((name, weights) -> {
            if (!weights.keySet().equals(COMPONENTS) || weights.values().stream().anyMatch(
                    w -> w == null || !Double.isFinite(w) || w < 0 || w > 1)
                    || Math.abs(weights.values().stream().mapToDouble(Double::doubleValue).sum() - 1) > 1e-9)
                throw new IllegalArgumentException("Profile weights must be finite, nonnegative and sum to 1");
            copy.put(name, Map.copyOf(weights));
        });
        if (!copy.keySet().equals(Set.of("BALANCED", "LATENCY_PRIORITY", "ENERGY_PRIORITY", "COST_PRIORITY")))
            throw new IllegalArgumentException("All four priority profiles are required");
        profiles = Map.copyOf(copy);
    }
    public Map<String, Double> weights(String profile) {
        String name = profile == null ? "BALANCED" : profile;
        Map<String, Double> result = profiles.get(name);
        if (result == null) throw new IllegalArgumentException("Unknown priority profile: " + name);
        return result;
    }
    public static ScoringConfiguration defaults() { return Defaults.VALUE; }
    private static class Defaults {
        static final ScoringConfiguration VALUE = load("classpath:decision-config.json");
    }
    public static ScoringConfiguration load(String location) {
        try (InputStream input = location.startsWith("classpath:")
                ? ScoringConfiguration.class.getResourceAsStream("/" + location.substring(10))
                : Files.newInputStream(Path.of(location))) {
            if (input == null) throw new IllegalArgumentException("Scoring configuration is missing");
            JsonNode root = JsonMapper.builder().build().readTree(input);
            Map<String, Bounds> bounds = new HashMap<>();
            Map<String, String> keys = Map.of("time", "execution_time_ms", "energy", "energy_j", "cost", "cost", "carbon", "carbon_g");
            keys.forEach((key, jsonKey) -> {
                JsonNode range = root.path("normalization").path(jsonKey);
                bounds.put(key, new Bounds(number(range.path("min")), number(range.path("max"))));
            });
            Map<String, Map<String, Double>> profiles = new HashMap<>();
            profiles.put("BALANCED", readWeights(root.path("weights")));
            for (String name : List.of("LATENCY_PRIORITY", "ENERGY_PRIORITY", "COST_PRIORITY")) {
                JsonNode node = root.path("profiles").path(name);
                // Python config has only balanced weights; Java profile defaults live in the bundled file.
                profiles.put(name, node.isMissingNode() ? defaults().weights(name) : readWeights(node));
            }
            return new ScoringConfiguration(bounds, profiles,
                number(root.path("device_adjustment").path("strength")),
                number(root.path("switching").path("minimum_improvement")),
                number(root.path("network_rules").path("max_latency_ms")),
                number(root.path("network_rules").path("min_bandwidth_mbps")));
        } catch (IOException ex) { throw new IllegalArgumentException("Cannot read scoring configuration", ex); }
    }
    private static double number(JsonNode node) {
        if (!node.isNumber()) throw new IllegalArgumentException("Configuration values must be numbers");
        return node.asDouble();
    }
    private static Map<String, Double> readWeights(JsonNode node) {
        return Map.of("time", number(node.path("execution_time")), "energy", number(node.path("energy")),
                      "cost", number(node.path("cost")), "carbon", number(node.path("carbon")));
    }
}
