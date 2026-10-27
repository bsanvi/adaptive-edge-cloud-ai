
package com.adaptive.decision.service;

import com.adaptive.decision.adapter.EstimatorRequestAdapter;
import com.adaptive.decision.client.EstimatorClient;
import com.adaptive.decision.dto.DecisionRequest;
import com.adaptive.decision.dto.EstimatorInputs;
import com.adaptive.decision.dto.EstimatorRequest;
import com.adaptive.decision.dto.EstimatorResponse;

import org.springframework.stereotype.Service;

@Service
public class EstimationGateway {

    private final EstimatorClient estimatorClient;

    public EstimationGateway(EstimatorClient estimatorClient) {
        this.estimatorClient = estimatorClient;
    }

    public EstimatorResponse estimate(
            DecisionRequest decisionRequest,
            EstimatorInputs inputs) {

        EstimatorRequest request =
            EstimatorRequestAdapter.adapt(
                decisionRequest, inputs
            );

        EstimatorResponse response =
            estimatorClient.estimate(request);

        if (response == null) {
            throw new IllegalStateException(
                "Estimator returned no response"
            );
        }

        if (!request.requestId().equals(
                response.requestId())) {
            throw new IllegalStateException(
                "Estimator response requestId mismatch"
            );
        }

        if (response.estimates() == null) {
            throw new IllegalStateException(
                "Estimator response contains no estimates"
            );
        }

        return response;
    }
}
