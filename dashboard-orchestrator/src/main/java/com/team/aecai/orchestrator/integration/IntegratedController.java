package com.team.aecai.orchestrator.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Profile("integration")
@RequestMapping("/api/v1/integrated")
public class IntegratedController {
    private final IntegratedOrchestrator orchestrator;
    private final IntegratedHistory history;
    private final IntegrationServiceStatus serviceStatus;
    public IntegratedController(IntegratedOrchestrator orchestrator, IntegratedHistory history, IntegrationServiceStatus serviceStatus) {
        this.orchestrator = orchestrator;
        this.history = history;
        this.serviceStatus = serviceStatus;
    }
    @GetMapping("/health")
    public Map<String,String> health() { return Map.of("status","UP","component","integrated-orchestrator"); }
    @GetMapping("/services")
    public Map<String,Boolean> services(){return serviceStatus.statuses();}
    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody IntegratedOrchestrator.RunTask task) {
        try { return ResponseEntity.ok(orchestrator.run(task)); }
        catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    }
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody IntegratedOrchestrator.RunTask task) {
        try { return ResponseEntity.ok(orchestrator.analyze(task)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (IllegalStateException e) { return ResponseEntity.status(409).body(Map.of("error", e.getMessage())); }
    }
    @GetMapping("/analysis/{id}")
    public ResponseEntity<?> analysis(@PathVariable("id") String id) {
        var saved = history.analysis(id);
        return saved == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(saved);
    }
    @PostMapping("/analysis/{id}/execute")
    public ResponseEntity<?> executeAnalysis(@PathVariable("id") String id) {
        try { return ResponseEntity.ok(orchestrator.executeAnalysis(id)); }
        catch (IllegalStateException e) { return ResponseEntity.status(409).body(Map.of("error", e.getMessage())); }
    }
    @GetMapping("/history")
    public List<Map<String,Object>> history(@RequestParam(name="limit", defaultValue="20") int limit){return history.recent(limit);}
    @GetMapping("/report/{id}")
    public ResponseEntity<?> report(@PathVariable("id") long id){
        if(id<1) return ResponseEntity.badRequest().body(Map.of("error","Invalid report ID"));
        var report=history.report(id);
        return report==null?ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Report not found for this run")):ResponseEntity.ok(report);
    }
}
