package com.edgecloud.edge.controller;

import com.edgecloud.edge.dto.EdgeEnergyEstimateRequest;
import com.edgecloud.edge.dto.EdgeEnergyEstimateResult;
import com.edgecloud.edge.service.EdgeEnergyEstimatorService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EdgeEnergyController {

    private final EdgeEnergyEstimatorService edgeEnergyEstimatorService;

    public EdgeEnergyController(
            EdgeEnergyEstimatorService edgeEnergyEstimatorService
    ) {
        this.edgeEnergyEstimatorService =
                edgeEnergyEstimatorService;
    }

    @PostMapping({
            "/edge/energy/estimate",
            "/api/v1/edge/energy/estimate"
    })
    public EdgeEnergyEstimateResult estimateEnergy(
            @RequestBody EdgeEnergyEstimateRequest request
    ) {
        return edgeEnergyEstimatorService.estimateEnergy(
                request
        );
    }
}
