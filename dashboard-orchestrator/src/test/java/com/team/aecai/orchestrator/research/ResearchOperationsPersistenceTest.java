package com.team.aecai.orchestrator.research;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResearchOperationsPersistenceTest {
    @Test void queuedExperimentPersistsAndCanBeCancelledAfterReconstruction(){
        String url="jdbc:h2:mem:p4-queue-"+System.nanoTime()+";DB_CLOSE_DELAY=-1";
        JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
        JsonMapper mapper=JsonMapper.builder().build();
        ResearchExperimentController engine=new ResearchExperimentController("http://127.0.0.1:8084","http://127.0.0.1:8001",0.02);
        ResearchOperationsStream stream=new ResearchOperationsStream(null);
        ResearchOperationsService service=new ResearchOperationsService(jdbc,mapper,engine,stream);
        String id="p4-persistence-test";
        Map<String,Object> scenario=new LinkedHashMap<>();
        scenario.put("taskId",id);scenario.put("cpuPercent",40.0);scenario.put("ramPercent",40.0);
        scenario.put("batteryPercent",85.0);scenario.put("latencyMs",15.0);scenario.put("bandwidthMbps",50.0);
        scenario.put("deadlineMs",2000.0);scenario.put("inputSizeBytes",65536L);
        scenario.put("edgeCapable",true);scenario.put("cloudCapable",true);scenario.put("privacyRequired",false);
        scenario.put("carbonGPerKwh",400.0);
        var request=new ResearchOperationsService.Submission("EXPERIMENT",id,null,null,null,null,null,
                scenario,Instant.now().plusSeconds(300).toString());
        var queued=service.submit(request);
        assertEquals("QUEUED",queued.get("status"));
        String jobId=String.valueOf(queued.get("id"));
        ResearchOperationsService restarted=new ResearchOperationsService(jdbc,mapper,engine,stream);
        assertEquals("QUEUED",restarted.job(jobId,false).get("status"));
        assertTrue(restarted.cancel(jobId));
        assertEquals("CANCELLED",restarted.job(jobId,false).get("status"));
        assertFalse(restarted.cancel(jobId));
    }
}
