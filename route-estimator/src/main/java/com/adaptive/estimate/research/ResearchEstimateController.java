package com.adaptive.estimate.research;

import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;
import static com.adaptive.estimate.research.Research.*;

@RestController
@RequestMapping("/api/v2/estimator")
public class ResearchEstimateController {
    private final CalibratedEstimator estimator;
    public ResearchEstimateController(CalibratedEstimator estimator) { this.estimator = estimator; }
    @PostMapping("/estimate") public Estimates estimate(@RequestBody EstimateInput input) { return estimator.estimate(input); }
    @PostMapping("/observations") public Map<String,Object> observe(@RequestBody Observation input) throws IOException {
        var s = estimator.observe(input); return Map.of("estimatorVersion", s.version(), "observations", s.observationIds().size());
    }
    @GetMapping("/calibration") public Map<String,Object> calibration() {
        var s = estimator.snapshot(); return Map.of("estimatorVersion", s.version(), "createdAt", s.createdAt(),
                "buckets", s.buckets(), "observations", s.observationIds().size(), "method", "EWMA alpha=0.2, minimum 5 matched observations");
    }
}
