package com.team.aecai.orchestrator.research;

import com.team.aecai.orchestrator.integration.IntegratedHistory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Augments existing H2 history with recorded provenance, never rewrites old decisions. */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/history")
public class ResearchHistoryController {
    private final JdbcTemplate jdbc;
    private final JsonMapper mapper;
    public ResearchHistoryController(JdbcTemplate jdbc,JsonMapper mapper,
                                     IntegratedHistory original,ResearchMetadataController metadata){
        this.jdbc=jdbc;this.mapper=mapper;
    }
    @GetMapping
    public ResponseEntity<List<Map<String,Object>>> recent(@RequestParam(defaultValue="100") int limit){
        List<Map<String,Object>> rows=jdbc.queryForList("""
            SELECT r.id, r.created_at, r.task_id, r.status, r.selected_route,
                   r.executed_route, r.fallback_used, r.prediction, r.confidence,
                   r.total_time_ms, r.error_text, r.report_json, m.metadata_json
              FROM integrated_runs r LEFT JOIN research_metadata m ON m.task_id=r.task_id
             ORDER BY r.id DESC LIMIT ?
            """,Math.min(100,Math.max(1,limit)));
        List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> row:rows){
            Map<String,Object> enriched=new LinkedHashMap<>(row);
            Object raw=row.get("METADATA_JSON");
            Map<String,Object> meta=Map.of();
            if(raw instanceof String json){
                try{
                    @SuppressWarnings("unchecked") Map<String,Object> parsed=mapper.readValue(json,Map.class);
                    if(parsed!=null)meta=parsed;
                }catch(Exception ignored){/* preserve old record, label unknown */}
            }
            enriched.remove("METADATA_JSON");
            Object reportJson=enriched.remove("REPORT_JSON");
            String snapshot=null;
            if(reportJson instanceof String str) snapshot=str;
            else if(reportJson instanceof java.sql.Clob clob) {
                try {long length=clob.length(); if(length>0 && length<=1_000_000) snapshot=clob.getSubString(1,(int)length);}
                catch(Exception ignored) { /* Older CLOB could not be read */ }
            }
            if(snapshot!=null){
                try{
                    @SuppressWarnings("unchecked") Map<String,Object> audit=mapper.readValue(snapshot,Map.class);
                    Object selected=enriched.get("EXECUTED_ROUTE");
                    if(selected==null)selected=enriched.get("SELECTED_ROUTE");
                    if(audit!=null&&selected instanceof String route){
                        Map<String,Object> routes=asMap(audit.get("estimates"));
                        Map<String,Object> estimate=asMap(routes.get(route));
                        if(Boolean.TRUE.equals(estimate.get("estimateAvailable"))){
                            enriched.put("ESTIMATED_LATENCY_MS",number(estimate.get("estimatedLatencyMs")));
                            enriched.put("ESTIMATED_ENERGY_J",number(estimate.get("energyEstimateJ")));
                            enriched.put("ESTIMATED_CARBON_G",number(estimate.get("carbonEstimateG")));
                            enriched.put("ESTIMATED_TRANSFER_MB",number(estimate.get("communicationMb")));
                        }
                    }
                    Map<String,Object> network=asMap(audit==null?null:audit.get("network"));
                    enriched.put("NETWORK_SOURCE",network.get("source"));
                    enriched.put("NETWORK_LATENCY_MS",number(network.get("latencyMs")));
                    enriched.put("NETWORK_BANDWIDTH_MBPS",number(network.get("bandwidthMbps")));
                }catch(Exception ignored){/* incomplete older report stays unestimated */}
            }
            enriched.put("DATA_SOURCE",meta.getOrDefault("source","UNRECORDED"));
            enriched.put("WORKLOAD_TYPE",meta.getOrDefault("workload","UNRECORDED"));
            enriched.put("TASK_NAME",meta.getOrDefault("name",""));
            result.add(enriched);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> asMap(Object value){
        return value instanceof Map<?,?> m?(Map<String,Object>)m:Map.of();
    }
    private static Double number(Object value){
        return value instanceof Number n&&Double.isFinite(n.doubleValue())?n.doubleValue():null;
    }
}
