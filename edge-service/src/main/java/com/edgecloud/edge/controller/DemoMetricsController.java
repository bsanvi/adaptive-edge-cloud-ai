package com.edgecloud.edge.controller;

import com.edgecloud.edge.dto.DemoMetricsRequest;
import com.edgecloud.edge.dto.DemoMetricsResponse;
import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.service.DemoMetricsService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/edge/demo/metrics")
public class DemoMetricsController {

    private final DemoMetricsService demoMetricsService;

    public DemoMetricsController(
            DemoMetricsService demoMetricsService
    ) {
        this.demoMetricsService = demoMetricsService;
    }

    @PostMapping
    public DemoMetricsResponse setDemoMetrics(
            @RequestBody DemoMetricsRequest request
    ) {

        EdgeMetrics metrics =
                demoMetricsService.setDemoMetrics(request);

        return new DemoMetricsResponse(
                true,
                "simulated",
                metrics
        );
    }

    @GetMapping
    public DemoMetricsResponse getDemoMetrics() {

        if (!demoMetricsService.isDemoMetricsActive()) {
            return new DemoMetricsResponse(
                    false,
                    "simulated",
                    null
            );
        }

        return new DemoMetricsResponse(
                true,
                "simulated",
                demoMetricsService.getDemoMetrics()
        );
    }

    @DeleteMapping
    public DemoMetricsResponse clearDemoMetrics() {

        demoMetricsService.clearDemoMetrics();

        return new DemoMetricsResponse(
                false,
                "simulated",
                null
        );
    }
}
