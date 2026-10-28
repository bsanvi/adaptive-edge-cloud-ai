package com.adaptive.decision.controller;
import com.adaptive.decision.dto.WhatIfRequest;
import com.adaptive.decision.service.WhatIfService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/v1/decision")
public class WhatIfController {
    private final WhatIfService service;
    public WhatIfController(WhatIfService service) { this.service = service; }
    @PostMapping("/what-if")
    public ResponseEntity<?> evaluate(@RequestBody WhatIfRequest request) {
        try { return ResponseEntity.ok(service.evaluate(request)); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); }
    }
}
