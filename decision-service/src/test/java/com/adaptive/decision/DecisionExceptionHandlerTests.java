
package com.adaptive.decision;

import com.adaptive.decision.controller.DecisionExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DecisionExceptionHandlerTests {

    @Test
    void returns503WhenEstimatorIsUnreachable() {

        var handler = new DecisionExceptionHandler();

        var exception = new ResourceAccessException(
            "Connection refused",
            new IOException("Estimator offline")
        );

        var response =
            handler.handleEstimatorConnectionError(exception);

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            response.getStatusCode()
        );

        assertEquals(
            "ESTIMATOR_UNAVAILABLE",
            response.getBody().get("status")
        );
    }

    @Test
    void returns503WhenEstimatorReturnsHttpError() {

        var handler = new DecisionExceptionHandler();

        var exception =
            new org.springframework.web.client
                .HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR
                );

        var response =
            handler.handleEstimatorHttpError(exception);

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            response.getStatusCode()
        );

        assertEquals(
            "ESTIMATOR_ERROR",
            response.getBody().get("status")
        );
    }

    @org.junit.jupiter.api.Test
    void unreadableEstimatorResponseReturnsControlledError() {
        var response = new com.adaptive.decision.controller.DecisionExceptionHandler()
            .handleEstimatorProtocolError(new org.springframework.web.client.RestClientException("invalid upstream JSON"));
        org.junit.jupiter.api.Assertions.assertEquals(503, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals("ESTIMATOR_ERROR", response.getBody().get("status"));
    }
}
