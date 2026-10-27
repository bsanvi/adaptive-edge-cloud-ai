
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;

public class UnconfiguredNetworkMetadataProvider
        implements NetworkMeasurementMetadataProvider {

    @Override
    public NetworkMeasurementMetadata resolve(
            DecisionRequest request) {

        throw new IllegalStateException(
            "Network measurement metadata is unavailable"
        );
    }
}
