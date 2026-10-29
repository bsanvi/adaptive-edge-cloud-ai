package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.EdgeEnergyEstimateRequest;
import com.edgecloud.edge.dto.EdgeEnergyEstimateResult;

import org.springframework.stereotype.Service;

@Service
public class EdgeEnergyEstimatorService {

    /*
     * Prototype assumptions.
     *
     * These values are NOT direct hardware measurements.
     * They provide a consistent model for comparing
     * experimental Edge executions.
     */
    private static final double IDLE_POWER_WATTS = 5.0;
    private static final double MAX_POWER_WATTS = 30.0;

    public EdgeEnergyEstimateResult estimateEnergy(
            EdgeEnergyEstimateRequest request
    ) {

        validateRequest(request);

        double averageCpuPercent =
                (
                        request.cpu_percent_before()
                                + request.cpu_percent_after()
                ) / 2.0;

        double cpuLoadFraction =
                averageCpuPercent / 100.0;

        double estimatedPowerWatts =
                IDLE_POWER_WATTS
                        + cpuLoadFraction
                        * (
                        MAX_POWER_WATTS
                                - IDLE_POWER_WATTS
                );

        double executionTimeSeconds =
                request.execution_time_ms()
                        / 1000.0;

        double estimatedEnergyJoules =
                estimatedPowerWatts
                        * executionTimeSeconds;

        return new EdgeEnergyEstimateResult(
                request.task_id(),
                round(
                        averageCpuPercent,
                        2
                ),
                round(
                        request.execution_time_ms(),
                        3
                ),
                round(
                        estimatedPowerWatts,
                        3
                ),
                round(
                        estimatedEnergyJoules,
                        6
                ),
                "estimated",
                "success"
        );
    }

    private void validateRequest(
            EdgeEnergyEstimateRequest request
    ) {

        if (request.execution_time_ms() < 0) {
            throw new IllegalArgumentException(
                    "Execution time cannot be negative"
            );
        }

        validateCpuPercent(
                request.cpu_percent_before(),
                "cpu_percent_before"
        );

        validateCpuPercent(
                request.cpu_percent_after(),
                "cpu_percent_after"
        );
    }

    private void validateCpuPercent(
            double value,
            String fieldName
    ) {

        if (
                value < 0.0
                        || value > 100.0
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be between 0 and 100"
            );
        }
    }

    private double round(
            double value,
            int decimals
    ) {

        double factor =
                Math.pow(
                        10,
                        decimals
                );

        return Math.round(
                value * factor
        ) / factor;
    }
}
