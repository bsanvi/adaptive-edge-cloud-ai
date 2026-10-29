package com.team.aecai.orchestrator.research;

import com.team.aecai.orchestrator.integration.IntegratedHistory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Full-database H2 aggregates, not merely the 100-row history API sample. */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/analytics")
public class ResearchAnalyticsController {
    private final JdbcTemplate jdbc;
    public ResearchAnalyticsController(JdbcTemplate jdbc,IntegratedHistory history){this.jdbc=jdbc;}
    @GetMapping
    public ResponseEntity<Map<String,Object>> summary(){
        Map<String,Object> raw=jdbc.queryForMap("""
           SELECT COUNT(*) AS n,
             COALESCE(SUM(CASE WHEN status='SUCCESS' THEN 1 ELSE 0 END),0) AS success_count,
             COALESCE(SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END),0) AS failure_count,
             COALESCE(SUM(CASE WHEN selected_route='EDGE' THEN 1 ELSE 0 END),0) AS edge_count,
             COALESCE(SUM(CASE WHEN selected_route='CLOUD' THEN 1 ELSE 0 END),0) AS cloud_count,
             COALESCE(SUM(CASE WHEN selected_route='HYBRID' THEN 1 ELSE 0 END),0) AS hybrid_count,
             COALESCE(SUM(CASE WHEN fallback_used THEN 1 ELSE 0 END),0) AS fallback_count,
             AVG(total_time_ms) AS avg_total_ms
           FROM integrated_runs
           """);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("source","H2_COMPLETE_HISTORY");
        result.put("total",getLong(raw,"N"));
        result.put("success",getLong(raw,"SUCCESS_COUNT"));
        result.put("failed",getLong(raw,"FAILURE_COUNT"));
        result.put("edge",getLong(raw,"EDGE_COUNT"));
        result.put("cloud",getLong(raw,"CLOUD_COUNT"));
        result.put("hybrid",getLong(raw,"HYBRID_COUNT"));
        result.put("fallback",getLong(raw,"FALLBACK_COUNT"));
        result.put("averageTotalMs",raw.get("AVG_TOTAL_MS") instanceof Number n?n.doubleValue():null);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    private static long getLong(Map<String,Object> m,String key){return m.get(key) instanceof Number n?n.longValue():0;}
}
