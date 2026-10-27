
package com.adaptive.decision.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class DecisionExceptionHandler {

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>>
            handleEstimatorConnectionError(
                ResourceAccessException exception) {

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", "ESTIMATOR_UNAVAILABLE",
                "message",
                "Route estimator could not be reached"
            ));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>>
            handleEstimatorHttpError(
                RestClientResponseException exception) {

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", "ESTIMATOR_ERROR",
                "message",
                "Route estimator returned an unsuccessful response"
            ));
    }
}
