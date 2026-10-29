package com.team.aecai.orchestrator.research;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;

/** Supplementary research provenance for integrated tasks, without touching the
 * existing H2 execution report or replacing historical decision snapshots. */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/metadata")
public class ResearchMetadataController {
    private final JdbcTemplate jdbc;
    private final JsonMapper mapper;
    public ResearchMetadataController(JdbcTemplate jdbc, JsonMapper mapper){
        this.jdbc=jdbc;this.mapper=mapper;
        jdbc.execute("CREATE TABLE IF NOT EXISTS research_metadata (task_id VARCHAR(100) PRIMARY KEY, metadata_json CLOB NOT NULL, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> save(@PathVariable String id,@RequestBody Map<String,Object> value){
        if (!valid(id) || value==null) return ResponseEntity.badRequest().body(Map.of("error","Invalid task ID or metadata"));
        try {
            String json=mapper.writeValueAsString(value);
            if(json.length()>16000) return ResponseEntity.badRequest().body(Map.of("error","Metadata too large"));
            jdbc.update("MERGE INTO research_metadata (task_id,metadata_json) KEY(task_id) VALUES (?,?)",id,json);
            return ResponseEntity.ok(Map.of("taskId",id,"saved",true));
        }catch(Exception e){return ResponseEntity.status(500).body(Map.of("error","Unable to save research metadata"));}
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id){
        if(!valid(id))return ResponseEntity.badRequest().body(Map.of("error","Invalid task ID"));
        var result=jdbc.query("SELECT metadata_json FROM research_metadata WHERE task_id=?",(rs,row)->rs.getString(1),id);
        if(result.isEmpty()) return ResponseEntity.status(404).body(Map.of("error","Metadata unavailable"));
        try{return ResponseEntity.ok(mapper.readValue(result.get(0),Map.class));}
        catch(Exception e){return ResponseEntity.status(500).body(Map.of("error","Stored metadata unreadable"));}
    }
    private boolean valid(String id){return id!=null&&id.length()<=100&&id.matches("[A-Za-z0-9_.-]{1,100}");}
}
