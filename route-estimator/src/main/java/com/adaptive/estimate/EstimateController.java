package com.adaptive.estimate;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/estimate")
public class EstimateController {
    private final RouteEstimateService service;
    public EstimateController(RouteEstimateService service){this.service=service;}
    @GetMapping("/health") public Map<String,String> health(){return Map.of("status","UP","service","route-estimator");}
    @PostMapping("/routes") public RouteEstimateService.Response routes(@Valid @RequestBody EstimateRequest body,HttpServletRequest request){
        request.setAttribute("requestId",body.requestId()); return service.estimate(body);
    }
}
