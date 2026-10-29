
package com.adaptive.decision.service;

import com.adaptive.decision.dto.DecisionRequest;

public interface DecisionInputsProvider {

    DecisionSupportingInputs resolve(
        DecisionRequest request
    );
}
