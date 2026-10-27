package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.DemoMetricsRequest;
import com.edgecloud.edge.dto.EdgeMetrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoMetricsServiceTest {

    @Test
    void shouldSetAndReturnDemoMetrics() {

        DemoMetricsService service =
                new DemoMetricsService();

        DemoMetricsRequest request =
                new DemoMetricsRequest(
                        80.0,
                        70.0,
                        11000,
                        5000,
                        25.0,
                        false
                );

        EdgeMetrics result =
                service.setDemoMetrics(request);

        assertTrue(service.isDemoMetricsActive());

        assertEquals(
                80.0,
                result.cpu_percent()
        );

        assertEquals(
                70.0,
                result.ram_percent()
        );

        assertEquals(
                25.0,
                result.battery_percent()
        );

        assertFalse(
                result.power_plugged()
        );
    }

    @Test
    void shouldClearDemoMetrics() {

        DemoMetricsService service =
                new DemoMetricsService();

        service.setDemoMetrics(
                new DemoMetricsRequest(
                        50.0,
                        50.0,
                        8000,
                        8000,
                        50.0,
                        true
                )
        );

        assertTrue(
                service.isDemoMetricsActive()
        );

        service.clearDemoMetrics();

        assertFalse(
                service.isDemoMetricsActive()
        );
    }

    @Test
    void shouldRejectInvalidCpuPercentage() {

        DemoMetricsService service =
                new DemoMetricsService();

        DemoMetricsRequest request =
                new DemoMetricsRequest(
                        120.0,
                        50.0,
                        8000,
                        8000,
                        50.0,
                        true
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.setDemoMetrics(request)
        );
    }

    @Test
    void shouldRejectInvalidBatteryPercentage() {

        DemoMetricsService service =
                new DemoMetricsService();

        DemoMetricsRequest request =
                new DemoMetricsRequest(
                        50.0,
                        50.0,
                        8000,
                        8000,
                        -5.0,
                        false
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.setDemoMetrics(request)
        );
    }
}
