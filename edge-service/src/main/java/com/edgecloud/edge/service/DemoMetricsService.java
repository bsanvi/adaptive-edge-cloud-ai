package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.DemoMetricsRequest;
import com.edgecloud.edge.dto.EdgeMetrics;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DemoMetricsService {

    private volatile EdgeMetrics demoMetrics;

    public EdgeMetrics setDemoMetrics(
            DemoMetricsRequest request
    ) {

        validate(request);

        EdgeMetrics metrics =
                new EdgeMetrics(
                        round(request.cpu_percent()),
                        round(request.ram_percent()),
                        request.ram_used_mb(),
                        request.ram_available_mb(),
                        round(request.battery_percent()),
                        request.power_plugged(),
                        Instant.now().toString()
                );

        demoMetrics = metrics;

        return metrics;
    }

    public EdgeMetrics getDemoMetrics() {

        if (demoMetrics == null) {
            throw new IllegalStateException(
                    "Controlled demo metrics are not active"
            );
        }

        return demoMetrics;
    }

    public boolean isDemoMetricsActive() {
        return demoMetrics != null;
    }

    public void clearDemoMetrics() {
        demoMetrics = null;
    }

    private void validate(
            DemoMetricsRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Demo metrics request cannot be null"
            );
        }

        validatePercentage(
                "cpu_percent",
                request.cpu_percent()
        );

        validatePercentage(
                "ram_percent",
                request.ram_percent()
        );

        validatePercentage(
                "battery_percent",
                request.battery_percent()
        );

        if (request.ram_used_mb() < 0) {
            throw new IllegalArgumentException(
                    "ram_used_mb cannot be negative"
            );
        }

        if (request.ram_available_mb() < 0) {
            throw new IllegalArgumentException(
                    "ram_available_mb cannot be negative"
            );
        }

        if (
                request.ram_used_mb()
                        + request.ram_available_mb()
                        <= 0
        ) {
            throw new IllegalArgumentException(
                    "RAM values cannot both be zero"
            );
        }
    }

    private void validatePercentage(
            String name,
            double value
    ) {

        if (
                Double.isNaN(value)
                        || Double.isInfinite(value)
                        || value < 0
                        || value > 100
        ) {
            throw new IllegalArgumentException(
                    name + " must be between 0 and 100"
            );
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
