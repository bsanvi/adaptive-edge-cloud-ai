
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;
import com.adaptive.decision.dto.EstimatorInputs;
import com.adaptive.decision.dto.RouteCostInputs;

import java.util.Map;
import java.util.Set;

public class ConfiguredDecisionInputsProvider
        implements DecisionInputsProvider {

    private static final Set<String> SOURCES =
        Set.of(
            "MEASURED",
            "SIMULATED",
            "ESTIMATED",
            "CALIBRATED"
        );

    private static final Set<String> ROUTES =
        Set.of("EDGE", "CLOUD", "HYBRID");

    private final DecisionPolicyConfig config;

    private final NetworkMeasurementMetadataProvider
        networkMetadataProvider;

    private final ExecutionCapabilitiesProvider
        capabilitiesProvider;

    private final String deviceSource;

    private final Map<String, Double> communicationCosts;

    public ConfiguredDecisionInputsProvider(
            DecisionPolicyConfig config,
            NetworkMeasurementMetadataProvider
                networkMetadataProvider,
            ExecutionCapabilitiesProvider
                capabilitiesProvider,
            String deviceSource,
            Map<String, Double> communicationCosts) {

        if (config == null
                || networkMetadataProvider == null
                || capabilitiesProvider == null) {

            throw new IllegalArgumentException(
                "Configuration and providers are required"
            );
        }

        if (!SOURCES.contains(deviceSource)) {
            throw new IllegalArgumentException(
                "Invalid device measurement source"
            );
        }

        if (communicationCosts == null
                || !communicationCosts.keySet()
                    .containsAll(ROUTES)) {

            throw new IllegalArgumentException(
                "Communication costs required for all routes"
            );
        }

        for (String route : ROUTES) {

            Double value = communicationCosts.get(route);

            if (value == null
                    || !Double.isFinite(value)
                    || value < 0) {

                throw new IllegalArgumentException(
                    "Invalid communication cost for " + route
                );
            }
        }

        this.config = config;
        this.networkMetadataProvider =
            networkMetadataProvider;
        this.capabilitiesProvider =
            capabilitiesProvider;

        this.deviceSource = deviceSource;

        this.communicationCosts =
            Map.copyOf(communicationCosts);
    }

    @Override
    public DecisionSupportingInputs resolve(
            DecisionRequest request) {

        if (request == null
                || request.task() == null
                || request.device() == null
                || request.network() == null) {

            throw new IllegalArgumentException(
                "Complete decision request is required"
            );
        }

        NetworkMeasurementMetadata metadata =
            networkMetadataProvider.resolve(request);

        ExecutionCapabilities capabilities =
            capabilitiesProvider.resolve(request);

        if (metadata == null || capabilities == null) {

            throw new IllegalStateException(
                "Supporting metadata or capabilities unavailable"
            );
        }

        EstimatorInputs estimatorInputs =
            new EstimatorInputs(
                config.estimatedFeatureBytes(),
                config.estimatedResultBytes(),
                capabilities.edgeCapable(),
                capabilities.cloudCapable(),
                config.edgeCarbonIntensityGPerKwh(),
                config.cloudCarbonIntensityGPerKwh(),
                metadata.sampledAt(),
                deviceSource,
                metadata.source()
            );

        return new DecisionSupportingInputs(
            estimatorInputs,
            new RouteCostInputs(
                config.monetaryCosts()
            ),
            communicationCosts,
            config.switchingCosts()
        );
    }
}
