package com.team.aecai.orchestrator.integration;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IntegratedReportHttpTest {
    @Test void numericReportIdSurvivesDatabaseReopenAndHttpBinding() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:http_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        var mapper = JsonMapper.builder().build();
        var writer = new IntegratedHistory(jdbc, mapper);
        writer.createTable();
        var result = new IntegratedOrchestrator.RunResult("http-report", "SUCCESS", "EDGE", "EDGE", false,
                "NORMAL", .8, 100, "Recorded", null, Map.of(), Map.of(), Map.of());
        long savedId = writer.save(result);
        var analysis = new IntegratedOrchestrator.Analysis(UUID.randomUUID().toString(), Instant.now().plusSeconds(60), result);
        writer.saveAnalysis(analysis);
        var reader = new IntegratedHistory(jdbc, mapper);
        reader.createTable();
        assertEquals(analysis.analysisId(), reader.analysis(analysis.analysisId()).get("analysisId"));
        var mvc = MockMvcBuilders.standaloneSetup(new IntegratedController(mock(IntegratedOrchestrator.class), reader,
                mock(IntegrationServiceStatus.class))).build();
        long id = ((Number) reader.recent(1).getFirst().get("ID")).longValue();
        assertEquals(savedId, id);
        mvc.perform(get("/api/v1/integrated/report/"+id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value("http-report"));
        mvc.perform(get("/api/v1/integrated/report/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/integrated/report/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/integrated/report/99999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/integrated/analysis/"+analysis.analysisId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(analysis.analysisId()));
    }
}
