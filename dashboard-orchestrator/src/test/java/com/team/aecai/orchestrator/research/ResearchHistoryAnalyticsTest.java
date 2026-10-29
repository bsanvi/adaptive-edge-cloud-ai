package com.team.aecai.orchestrator.research;

import com.team.aecai.orchestrator.integration.IntegratedHistory;
import com.team.aecai.orchestrator.integration.IntegratedOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** H2 integration checks without external services, production Decision Engine or mocked scores. */
class ResearchHistoryAnalyticsTest {
    @Test void completeHistoryCountsAndProvenanceAreReadFromH2() {
        String db="jdbc:h2:mem:research_"+UUID.randomUUID().toString().replace("-", "")+";DB_CLOSE_DELAY=-1";
        JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource(db,"sa",""));
        JsonMapper mapper=JsonMapper.builder().build();
        IntegratedHistory history=new IntegratedHistory(jdbc,mapper);
        history.createTable();
        ResearchMetadataController metadata=new ResearchMetadataController(jdbc,mapper);
        history.save(new IntegratedOrchestrator.RunResult("test-task","SUCCESS","EDGE","EDGE",false,
                "NORMAL",0.9,23.5,"Private task chose EDGE",null,Map.of("status","selected"),Map.of("EDGE", Map.of("estimateAvailable",true,"estimatedLatencyMs",12.0,"energyEstimateJ",0.16,"carbonEstimateG",0.0003,"communicationMb",0.0)),Map.of("source","MEASURED_APPROXIMATE","latencyMs",18.0,"bandwidthMbps",20.0)));
        assertEquals(200,metadata.save("test-task", Map.of("source","DEMO SCENARIO","workload","sensor_anomaly","name","Research task")).getStatusCode().value());
        var summary=new ResearchAnalyticsController(jdbc,history).summary().getBody();
        assertNotNull(summary);
        assertEquals(1L,summary.get("total"));
        assertEquals(1L,summary.get("success"));
        assertEquals(1L,summary.get("edge"));
        var rows=new ResearchHistoryController(jdbc,mapper,history,metadata).recent(10).getBody();
        assertNotNull(rows);
        assertEquals(1,rows.size());
        assertEquals("DEMO SCENARIO",rows.get(0).get("DATA_SOURCE"));
        assertEquals("Research task",rows.get(0).get("TASK_NAME"));
        assertEquals(0.16, (Double)rows.get(0).get("ESTIMATED_ENERGY_J"), 0.000001);
        assertEquals(12.0, (Double)rows.get(0).get("ESTIMATED_LATENCY_MS"), 0.000001);
    }
}
