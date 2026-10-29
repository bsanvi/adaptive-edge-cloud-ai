package com.adaptive.decision.service;
import com.adaptive.decision.dto.DecisionRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;
import java.util.Optional;

@Service
public class SupportingInputsGate implements DecisionInputsProvider {
    private final boolean enabled;
    private final Optional<DecisionIntegrationPolicy> policy;
    private final Optional<NetworkMeasurementMetadataProvider> network;
    private final Optional<ExecutionCapabilitiesProvider> capabilities;
    private final Optional<DeviceProvenanceProvider> device;
    public SupportingInputsGate(@Value("${decision.supporting-inputs.enabled:false}") boolean enabled,
            Optional<DecisionIntegrationPolicy> policy, Optional<NetworkMeasurementMetadataProvider> network,
            Optional<ExecutionCapabilitiesProvider> capabilities, Optional<DeviceProvenanceProvider> device) {
        this.enabled = enabled; this.policy = policy; this.network = network;
        this.capabilities = capabilities; this.device = device;
    }
    @Override public DecisionSupportingInputs resolve(DecisionRequest request) {
        if (!enabled || policy.isEmpty() || network.isEmpty() || capabilities.isEmpty() || device.isEmpty())
            return new UnconfiguredDecisionInputsProvider().resolve(request);
        if (request == null || request.device() == null) throw new IllegalArgumentException("Device metrics required");
        var provenance = device.get().resolve(request);
        var metadata = network.get().resolve(request);
        var capable = capabilities.get().resolve(request);
        if (provenance == null || metadata == null || capable == null
                || !"MEASURED".equals(provenance.source()) || !"MEASURED".equals(metadata.source())
                || !java.util.Objects.equals(request.device().timestamp(), provenance.sampledAt())
                || !fresh(provenance.sampledAt()) || !fresh(metadata.sampledAt()))
            throw new IllegalStateException("Fresh authoritative measured metadata and capabilities are required");
        var configured = new ConfiguredDecisionInputsProvider(policy.get().policy(), r -> metadata, r -> capable,
                provenance.source(), policy.get().communicationCosts());
        return configured.resolve(request);
    }
    private boolean fresh(Instant timestamp) {
        if (timestamp == null) return false;
        Duration age = Duration.between(timestamp, Instant.now());
        return !age.isNegative() && age.compareTo(Duration.ofSeconds(5)) <= 0;
    }
}
