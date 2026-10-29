package com.edgecloud.edge.research;

import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.service.SystemMonitorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static com.edgecloud.edge.research.Research.Capability;
import static com.edgecloud.edge.research.Research.Device;

@RestController
@RequestMapping("/api/v2/edge")
public class ResearchEdgeController {

    private final ImageModel imageModel;
    private final SystemMonitorService systemMonitorService;

    public ResearchEdgeController(
            ImageModel imageModel,
            SystemMonitorService systemMonitorService
    ) {
        this.imageModel = imageModel;
        this.systemMonitorService = systemMonitorService;
    }

    @GetMapping("/ready")
    public Capability ready() {
        return imageModel.capability();
    }

    @GetMapping("/telemetry")
    public Device telemetry() {

        EdgeMetrics metrics =
                systemMonitorService.getEdgeMetrics();

        Capability capability =
                imageModel.capability();

        return new Device(
                metrics.cpu_percent(),
                metrics.ram_percent(),
                metrics.battery_percent(),
                metrics.power_plugged(),
                Instant.parse(metrics.timestamp()),
                "LIVE_MEASUREMENT",
                "OSHI system CPU, RAM, battery and power-state measurement",
                capability.environmentId()
        );
    }
}