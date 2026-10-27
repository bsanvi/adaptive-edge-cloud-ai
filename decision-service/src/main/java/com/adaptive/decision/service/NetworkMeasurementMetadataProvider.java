
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;

public interface NetworkMeasurementMetadataProvider {

    NetworkMeasurementMetadata resolve(
        DecisionRequest request
    );
}
