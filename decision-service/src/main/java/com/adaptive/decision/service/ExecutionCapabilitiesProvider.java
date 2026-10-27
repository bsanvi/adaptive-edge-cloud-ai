
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;

public interface ExecutionCapabilitiesProvider {

    ExecutionCapabilities resolve(
        DecisionRequest request
    );
}
