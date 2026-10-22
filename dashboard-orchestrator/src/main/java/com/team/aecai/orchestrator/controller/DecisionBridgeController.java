
package com.team.aecai.orchestrator.controller;

import com.team.aecai.orchestrator.client.PythonDecisionApiClient;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("integration")
@RequestMapping("/api/v1/integration/decision")
public class DecisionBridgeController {

    private final PythonDecisionApiClient decisionClient;

    public DecisionBridgeController(
            PythonDecisionApiClient decisionClient) {

        this.decisionClient = decisionClient;
    }

    @PostMapping(
        value = "/evaluate",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> evaluate(
            @RequestBody Map<String, Object> request) {

        return decisionClient.evaluate(request);
    }
}
