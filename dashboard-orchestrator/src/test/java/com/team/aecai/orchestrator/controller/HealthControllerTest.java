
package com.team.aecai.orchestrator.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void healthReturnsExpectedStatus() {
        HealthController controller = new HealthController();

        Map<String, String> result = controller.health();

        assertEquals("ok", result.get("status"));
        assertEquals("orchestrator", result.get("component"));
    }
}
