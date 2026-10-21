package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.EdgeMetrics;

import org.springframework.stereotype.Service;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;

import java.time.Instant;
import java.util.List;

@Service
public class SystemMonitorService {

    private final HardwareAbstractionLayer hardware;

    public SystemMonitorService() {
        SystemInfo systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
    }

    public EdgeMetrics getEdgeMetrics() {

        // CPU usage
        CentralProcessor processor = hardware.getProcessor();

        double cpuPercent =
                processor.getSystemCpuLoad(1000L) * 100.0;

        // RAM usage
        GlobalMemory memory = hardware.getMemory();

        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;

        double ramPercent =
                totalMemory == 0
                        ? 0.0
                        : ((double) usedMemory / totalMemory) * 100.0;

        long ramUsedMb = bytesToMb(usedMemory);
        long ramAvailableMb = bytesToMb(availableMemory);

        // Battery information
        Double batteryPercent = null;
        Boolean powerPlugged = null;

        List<PowerSource> powerSources = hardware.getPowerSources();

        if (!powerSources.isEmpty()) {
            PowerSource battery = powerSources.get(0);

            batteryPercent =
                    battery.getRemainingCapacityPercent() * 100.0;

            powerPlugged =
                    battery.isPowerOnLine();
        }

        return new EdgeMetrics(
                round(cpuPercent),
                round(ramPercent),
                ramUsedMb,
                ramAvailableMb,
                batteryPercent == null
                        ? null
                        : round(batteryPercent),
                powerPlugged,
                Instant.now().toString()
        );
    }

    private long bytesToMb(long bytes) {
        return bytes / (1024 * 1024);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}