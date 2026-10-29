package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.EdgeEnergyEstimateRequest;
import com.edgecloud.edge.dto.EdgeEnergyEstimateResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EdgeEnergyEstimatorServiceTest {

    @Test
    void shouldEstimateEnergySuccessfully() {

        EdgeEnergyEstimatorService service =
                new EdgeEnergyEstimatorService();

        EdgeEnergyEstimateRequest request =
                new EdgeEnergyEstimateRequest(
                        "TASK-301",
                        100.0,
                        20.0,
                        40.0
                );

        EdgeEnergyEstimateResult result =
                service.estimateEnergy(request);

        assertEquals(
                "TASK-301",
                result.task_id()
        );

        assertEquals(
                30.0,
                result.average_cpu_percent()
        );

        assertEquals(
                100.0,
                result.execution_time_ms()
        );

        assertEquals(
                12.5,
                result.estimated_power_watts()
        );

        assertEquals(
                1.25,
                result.estimated_energy_joules()
        );

        assertEquals(
                "estimated",
                result.measurement_type()
        );

        assertEquals(
                "success",
                result.status()
        );
    }

    @Test
    void shouldRejectNegativeExecutionTime() {

        EdgeEnergyEstimatorService service =
                new EdgeEnergyEstimatorService();

        EdgeEnergyEstimateRequest request =
                new EdgeEnergyEstimateRequest(
                        "TASK-302",
                        -10.0,
                        20.0,
                        30.0
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.estimateEnergy(request)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Execution time cannot be negative"
                        )
        );
    }

    @Test
    void shouldRejectInvalidCpuPercentage() {

        EdgeEnergyEstimatorService service =
                new EdgeEnergyEstimatorService();

        EdgeEnergyEstimateRequest request =
                new EdgeEnergyEstimateRequest(
                        "TASK-303",
                        100.0,
                        120.0,
                        30.0
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.estimateEnergy(request)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "cpu_percent_before"
                        )
        );
    }
}
