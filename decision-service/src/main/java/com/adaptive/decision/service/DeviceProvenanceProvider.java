package com.adaptive.decision.service;
import com.adaptive.decision.dto.DecisionRequest;
import java.time.Instant;
/** Implement using authoritative measurement context, never a generated current timestamp. */
public interface DeviceProvenanceProvider {
    record Provenance(Instant sampledAt, String source) {}
    Provenance resolve(DecisionRequest request);
}
