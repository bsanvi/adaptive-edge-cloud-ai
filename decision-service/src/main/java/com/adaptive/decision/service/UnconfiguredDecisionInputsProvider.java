
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;


public class UnconfiguredDecisionInputsProvider
        implements DecisionInputsProvider {

    @Override
    public DecisionSupportingInputs resolve(
            DecisionRequest request) {

        throw new IllegalStateException(
            "Decision supporting inputs are not configured. "
            + "Estimator metadata and cost policies "
            + "must be supplied before live routing."
        );
    }
}
