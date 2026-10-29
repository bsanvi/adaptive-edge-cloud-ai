package com.team.aecai.orchestrator.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Separate controller: existing integrated API contracts remain unchanged. */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/integrated")
public class IntegrationTelemetryController {
    private final IntegrationTelemetry telemetry;

    public IntegrationTelemetryController(IntegrationTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @GetMapping("/telemetry")
    public ResponseEntity<Map<String, Object>> telemetry() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(telemetry.snapshot());
    }
}
