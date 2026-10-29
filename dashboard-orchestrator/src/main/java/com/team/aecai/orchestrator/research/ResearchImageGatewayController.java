package com.team.aecai.orchestrator.research;

import com.team.aecai.orchestrator.integration.IntegratedHistory;
import com.team.aecai.orchestrator.integration.IntegratedOrchestrator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/** Secure same-origin upload gateway. Sends binary to Edge; never trusts browser paths. */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/images")
public class ResearchImageGatewayController {
    private final String edgeUrl;
    private final JsonMapper mapper;
    private final IntegratedHistory history;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    public ResearchImageGatewayController(@Value("${integration.edge-url:http://127.0.0.1:8081}") String url,JsonMapper mapper,IntegratedHistory history,JdbcTemplate jdbc){
        this.edgeUrl=url.replaceAll("/+$","");this.mapper=mapper;this.history=history;
        jdbc.execute("ALTER TABLE integrated_runs ALTER COLUMN prediction VARCHAR(255)");
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("image") MultipartFile image,
            @RequestParam("taskId") String taskId,@RequestParam("deadlineMs") double deadlineMs,
            @RequestParam("privacyLevel") String privacyLevel,
            @RequestParam("priorityProfile") String priorityProfile){
        if(image.isEmpty()||image.getSize()>10*1024*1024||image.getSize()==0)
            return ResponseEntity.badRequest().body(Map.of("error","Image must be between 1 byte and 10MB"));
        if(!taskId.matches("[A-Za-z0-9_.-]{1,100}")||!Double.isFinite(deadlineMs)||deadlineMs<=0||deadlineMs>10_000_000)
            return ResponseEntity.badRequest().body(Map.of("error","Invalid task ID or deadline"));
        try {
            String boundary="----Edgecloud"+UUID.randomUUID().toString().replace("-","");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            String ext=image.getOriginalFilename()!=null&&image.getOriginalFilename().toLowerCase().endsWith(".png")?".png":".jpg";
            writePart(out,boundary,"taskId",taskId);
            writePart(out,boundary,"deadlineMs",String.valueOf(deadlineMs));
            writePart(out,boundary,"privacyLevel",privacyLevel);
            writePart(out,boundary,"priorityProfile",priorityProfile);
            out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"image\"; filename=\"sample"+ext+"\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.write(image.getBytes());
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            out.write(("--"+boundary+"--\r\n").getBytes(StandardCharsets.US_ASCII));
            HttpRequest req=HttpRequest.newBuilder(URI.create(edgeUrl+"/api/v1/edge/image/upload"))
                    .timeout(Duration.ofSeconds(20)).header("Content-Type","multipart/form-data; boundary="+boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build();
            HttpResponse<String> result=client.send(req,HttpResponse.BodyHandlers.ofString());
            return proxy(result);
        }catch(Exception e){return ResponseEntity.status(503).body(Map.of("error","Secure image bridge on Edge unavailable; merge Person 1 profiler and install the optional bridge"));}
    }
    @PostMapping("/{ref}/classify")
    public ResponseEntity<?> classify(@PathVariable String ref){
        if(!ref.matches("[a-fA-F0-9-]{36}"))return ResponseEntity.badRequest().body(Map.of("error","Invalid image reference"));
        try {
            long started=System.nanoTime();
            HttpRequest req=HttpRequest.newBuilder(URI.create(edgeUrl+"/api/v1/edge/image/classify/"+ref))
                .timeout(Duration.ofSeconds(25)).POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> upstream=client.send(req,HttpResponse.BodyHandlers.ofString());
            ResponseEntity<?> response=proxy(upstream);
            if(upstream.statusCode()>=200&&upstream.statusCode()<300 && response.getBody() instanceof Map<?,?> payload){
                Object task=payload.get("taskId");
                if(task instanceof String taskId && taskId.matches("[A-Za-z0-9_.-]{1,100}")){
                    double elapsed=(System.nanoTime()-started)/1_000_000.0;
                    var report=new IntegratedOrchestrator.RunResult(taskId,"SUCCESS",null,"EDGE",false,
                        String.valueOf(payload.get("predicted_label")),
                        payload.get("confidence") instanceof Number n?n.doubleValue():null,
                        elapsed,"Edge-only image classification using Person 1's SqueezeNet model. No Cloud/Hybrid image model or adaptive Decision Engine route was used.",
                        null,Map.of("status","NOT_EVALUATED","reason","Image classifer has no compatible Cloud/Hybrid model in this integration"),
                        Map.of(),Map.of("source","NOT_PROBED"));
                    history.save(report);
                }
            }
            return response;
        }
        catch(Exception e){return ResponseEntity.status(503).body(Map.of("error","Edge image classifier unavailable"));}
    }
    private static void writePart(ByteArrayOutputStream out,String boundary,String name,String value)throws Exception{
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+name+"\"\r\n\r\n"+value+"\r\n").getBytes(StandardCharsets.US_ASCII));
    }
    @SuppressWarnings("unchecked") private ResponseEntity<?> proxy(HttpResponse<String> r){
        try {
            Map<String,Object> body=mapper.readValue(r.body(),Map.class);
            if(r.statusCode()>=200&&r.statusCode()<300)return ResponseEntity.ok(body);
            if(r.statusCode()==404)return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
            if(r.statusCode()>=400&&r.statusCode()<500)return ResponseEntity.badRequest().body(body);
            return ResponseEntity.status(502).body(Map.of("error","Edge bridge failed"));
        }catch(Exception e){return ResponseEntity.status(502).body(Map.of("error","Invalid Edge bridge response"));}
    }
}
