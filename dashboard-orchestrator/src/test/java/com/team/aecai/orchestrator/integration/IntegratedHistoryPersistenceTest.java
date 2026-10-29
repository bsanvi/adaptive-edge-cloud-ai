package com.team.aecai.orchestrator.integration;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Verify that the full report JSON survives constructing a new repository instance. */
class IntegratedHistoryPersistenceTest {
    @Test
    void reportCanBeReopenedAndMatchesHistoryRow() {
        var datasource=new DriverManagerDataSource();
        datasource.setDriverClassName("org.h2.Driver");
        datasource.setUrl("jdbc:h2:mem:p4_"+UUID.randomUUID().toString().replace("-","")+";DB_CLOSE_DELAY=-1");
        datasource.setUsername("sa");
        datasource.setPassword("");
        JdbcTemplate jdbc=new JdbcTemplate(datasource);
        JsonMapper mapper=JsonMapper.builder().build();
        IntegratedHistory writer=new IntegratedHistory(jdbc,mapper);
        writer.createTable();
        var run=new IntegratedOrchestrator.RunResult(
                "persist-001","SUCCESS","EDGE","EDGE",false,
                "NORMAL",0.81,123.4,"Privacy-safe Edge route",null,
                Map.of("status","selected","selected_location","edge"),
                Map.of("EDGE",Map.of("estimatedLatencyMs",10.0,"estimateAvailable",true)),
                Map.of("connected",true,"latencyMs",12.0,"bandwidthMbps",40.0));
        writer.save(run);
        var recent=writer.recent(5);
        assertEquals(1,recent.size());
        assertEquals("persist-001",recent.get(0).get("TASK_ID"));
        long id=((Number)recent.get(0).get("ID")).longValue();
        // A new object reads the saved JSON from H2, rather than a UI session cache.
        IntegratedHistory reader=new IntegratedHistory(jdbc,mapper);
        reader.createTable();
        Map<String,Object> report=reader.report(id);
        assertNotNull(report);
        assertEquals("EDGE",report.get("selectedRoute"));
        assertEquals("NORMAL",report.get("prediction"));
        assertEquals("Privacy-safe Edge route",report.get("explanation"));
        assertTrue(report.containsKey("decision"));
        assertNull(reader.report(id+12345));
    }
}
