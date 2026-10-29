package com.team.aecai.orchestrator.research;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/operations")
public class ResearchOperationsController {
    private final ResearchOperationsService operations;
    private final ResearchOperationsAuth auth;
    private final ResearchOperationsStream streaming;
    public ResearchOperationsController(ResearchOperationsService operations,ResearchOperationsAuth auth,
                                        ResearchOperationsStream streaming){
        this.operations=operations;this.auth=auth;this.streaming=streaming;
    }
    /** Only status metadata; request/result payloads are not made public. */
    @GetMapping("/jobs")
    public ResponseEntity<?> jobs(@RequestParam(defaultValue="50") int limit){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(operations.jobs(limit));
    }
    @GetMapping("/jobs/{id}")
    public ResponseEntity<?> job(@PathVariable String id,HttpServletRequest request){
        boolean authorized=false;
        try{auth.require(request);authorized=true;}catch(Exception ignored){/* public status only */}
        Map<String,Object> result=operations.job(id,authorized);
        return result==null?ResponseEntity.notFound().build():ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    @PostMapping(value="/jobs",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submit(@RequestBody ResearchOperationsService.Submission item,HttpServletRequest request){
        auth.require(request);
        try{return ResponseEntity.accepted().body(operations.submit(item));}
        catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
        catch(IllegalStateException e){return ResponseEntity.status(429).body(Map.of("error",e.getMessage()));}
    }
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> cancel(@PathVariable String id,HttpServletRequest request){
        auth.require(request);
        if(!operations.cancel(id))return ResponseEntity.status(409).body(Map.of("error","Job is not queued or does not exist"));
        return ResponseEntity.ok(Map.of("id",id,"status","CANCELLED"));
    }
    @GetMapping(path="/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(){return streaming.register(false);}
    @GetMapping(path="/telemetry/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter telemetry(){return streaming.register(true);}
}
