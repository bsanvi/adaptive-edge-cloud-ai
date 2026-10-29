package com.team.aecai.orchestrator.research;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * H2-backed bounded, single-worker queue. An interruption is NOT automatically retried,
 * because the remote execution may have succeeded before the process went down.
 * Experiments call the existing estimator/Decision Engine; queued real executions require
 * human review in the existing Analyze -> Review -> Execute-once workflow.
 */
@Service
@Profile("integration")
public class ResearchOperationsService {
    public record Submission(String kind, String taskId, List<Double> samples,
                             Double deadlineMs, Boolean privacyRequired, String complexity,
                             String previousRoute, Map<String,Object> scenario, String runAt) {}

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final ResearchExperimentController experiments;
    private final ResearchOperationsStream stream;

    public ResearchOperationsService(JdbcTemplate jdbc, JsonMapper json,
            ResearchExperimentController experiments, ResearchOperationsStream stream) {
        this.jdbc=jdbc; this.json=json; this.experiments=experiments; this.stream=stream;
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS research_operation_jobs (
                  id VARCHAR(36) PRIMARY KEY,
                  kind VARCHAR(12) NOT NULL,
                  task_id VARCHAR(100) NOT NULL,
                  status VARCHAR(18) NOT NULL,
                  payload_json CLOB NOT NULL,
                  result_json CLOB,
                  error_text VARCHAR(1000),
                  report_id BIGINT,
                  run_at_ms BIGINT NOT NULL,
                  created_at_ms BIGINT NOT NULL,
                  updated_at_ms BIGINT NOT NULL
                )
                """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_research_jobs_due ON research_operation_jobs(status,run_at_ms)");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ix_research_jobs_task_unique ON research_operation_jobs(task_id)");
        jdbc.update("UPDATE research_operation_jobs SET status='INTERRUPTED',error_text=?,updated_at_ms=? WHERE status='RUNNING'",
                "Worker stopped mid-operation; review the execution history before any manual resubmission", System.currentTimeMillis());
    }

    static void validate(Submission item) {
        if(item==null || item.kind()==null || !Set.of("EXPERIMENT","EXECUTION").contains(item.kind())
                || item.taskId()==null || !item.taskId().matches("[A-Za-z0-9_.-]{1,100}"))
            throw new IllegalArgumentException("Invalid job kind or task ID");
        if("EXECUTION".equals(item.kind())) {
            if(item.samples()==null || item.samples().size()<8 || item.samples().size()>4096
                    || item.samples().stream().anyMatch(v->v==null || !Double.isFinite(v) || Math.abs(v)>1_000_000)
                    || item.deadlineMs()==null || !Double.isFinite(item.deadlineMs())
                    || item.deadlineMs()<=0 || item.deadlineMs()>10_000_000
                    || item.privacyRequired()==null || item.complexity()==null || !Set.of("LOW","MEDIUM","HIGH").contains(item.complexity())
                    || (item.previousRoute()!=null && !Set.of("EDGE","CLOUD","HYBRID").contains(item.previousRoute())))
                throw new IllegalArgumentException("Invalid execution job: finite samples, privacy, complexity and deadline required");
        } else if(item.scenario()==null || item.scenario().size()>20) {
            throw new IllegalArgumentException("Experiment needs a valid scenario payload");
        }
    }

    static long scheduledTime(String runAt,long now) {
        if(runAt==null || runAt.isBlank())return now;
        try {
            long requested=Instant.parse(runAt).toEpochMilli();
            if(requested<now-60_000 || requested>now+30L*24*3600*1000)
                throw new IllegalArgumentException("Schedule must be within the next 30 days");
            return Math.max(now,requested);
        }catch(DateTimeParseException e){throw new IllegalArgumentException("runAt must be ISO-8601 UTC time",e);}
    }

    public Map<String,Object> submit(Submission item) {
        validate(item);
        if("EXPERIMENT".equals(item.kind())) {
            try {
                ResearchExperimentController.Scenario scenario = json.readValue(json.writeValueAsString(item.scenario()),
                        ResearchExperimentController.Scenario.class);
                ResearchExperimentController.validate(scenario);
                if(!item.taskId().equals(scenario.taskId()))
                    throw new IllegalArgumentException("Job ID and scenario taskId must match");
            }catch(IllegalArgumentException e){throw e;}
            catch(Exception e){throw new IllegalArgumentException("Invalid experiment scenario",e);}
        }
        long now=System.currentTimeMillis();
        long due=scheduledTime(item.runAt(),now);
        Long pending=jdbc.queryForObject("SELECT COUNT(*) FROM research_operation_jobs WHERE status IN ('QUEUED','RUNNING','REVIEW_REQUIRED')",Long.class);
        if(pending!=null&&pending>=100)throw new IllegalStateException("Research queue is full (100 pending/review jobs)");
        Long duplicate=jdbc.queryForObject("SELECT COUNT(*) FROM research_operation_jobs WHERE task_id=?",Long.class,item.taskId());
        if(duplicate!=null&&duplicate>0)throw new IllegalArgumentException("Task ID already queued: create a new task ID");
        if("EXECUTION".equals(item.kind())) {
            Long recorded=jdbc.queryForObject("SELECT COUNT(*) FROM integrated_runs WHERE task_id=?",Long.class,item.taskId());
            if(recorded!=null&&recorded>0)throw new IllegalArgumentException("Task ID already recorded; duplicate execution prevented");
        }
        String id=UUID.randomUUID().toString();
        String payload=json.writeValueAsString(item);
        if(payload.length()>90_000)throw new IllegalArgumentException("Job payload too large");
        jdbc.update("INSERT INTO research_operation_jobs (id,kind,task_id,status,payload_json,run_at_ms,created_at_ms,updated_at_ms) VALUES (?,?,?,'QUEUED',?,?,?,?)",
                id,item.kind(),item.taskId(),payload,due,now,now);
        Map<String,Object> response=job(id,false);
        stream.publish(response);
        return response;
    }

    public List<Map<String,Object>> jobs(int limit) {
        return jdbc.query("SELECT * FROM research_operation_jobs ORDER BY created_at_ms DESC LIMIT ?",
                (rs,i)->toPublicRow(rs.getString("id"),rs.getString("kind"),rs.getString("task_id"),
                        rs.getString("status"),rs.getLong("run_at_ms"),rs.getLong("created_at_ms"),
                        rs.getLong("updated_at_ms"),rs.getObject("report_id"),rs.getString("error_text")),
                Math.max(1,Math.min(100,limit)));
    }

    private static Map<String,Object> toPublicRow(String id,String kind,String taskId,String status,
            long runAt,long created,long updated,Object reportId,String error) {
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",id);m.put("kind",kind);m.put("taskId",taskId);m.put("status",status);
        m.put("runAt",Instant.ofEpochMilli(runAt).toString());
        m.put("createdAt",Instant.ofEpochMilli(created).toString());
        m.put("updatedAt",Instant.ofEpochMilli(updated).toString());
        m.put("reportId",reportId);m.put("error",error);
        m.put("source",kind.equals("EXPERIMENT")?"SIMULATED_INPUT_REAL_ENGINE":"BACKEND_EXECUTION");
        return m;
    }

    public Map<String,Object> job(String id,boolean includeResult) {
        if(id==null || !id.matches("[a-fA-F0-9-]{36}"))return null;
        List<Map<String,Object>> result=jdbc.query("SELECT * FROM research_operation_jobs WHERE id=?",
                (rs,row)->{
                    Map<String,Object> m=toPublicRow(rs.getString("id"),rs.getString("kind"),rs.getString("task_id"),
                            rs.getString("status"),rs.getLong("run_at_ms"),rs.getLong("created_at_ms"),
                            rs.getLong("updated_at_ms"),rs.getObject("report_id"),rs.getString("error_text"));
                    if(includeResult){
                        try{m.put("request",json.readValue(rs.getString("payload_json"),Map.class));}
                        catch(Exception ignored){m.put("requestUnavailable",true);}
                        if(rs.getString("result_json")!=null){
                            try{m.put("result",json.readValue(rs.getString("result_json"),Map.class));}
                            catch(Exception ignored){m.put("resultUnavailable",true);}
                        }
                    }
                    return m;
                },id);
        return result.isEmpty()?null:result.get(0);
    }

    public boolean cancel(String id) {
        int count=jdbc.update("UPDATE research_operation_jobs SET status='CANCELLED',updated_at_ms=? WHERE id=? AND status='QUEUED'",
                System.currentTimeMillis(),id);
        if(count>0)stream.publish(job(id,false));
        return count>0;
    }

    @Scheduled(fixedDelay=1500,initialDelay=2500)
    public void processDueJob() {
        // Reconcile user-approved executions after their real H2 audit is written.
        reconcileReviewedExecutions();
        List<String> due=jdbc.query("SELECT id FROM research_operation_jobs WHERE status='QUEUED' AND run_at_ms<=? ORDER BY run_at_ms,created_at_ms LIMIT 1",
                (rs,i)->rs.getString(1),System.currentTimeMillis());
        if(due.isEmpty())return;
        String id=due.get(0);
        if(jdbc.update("UPDATE research_operation_jobs SET status='RUNNING',updated_at_ms=? WHERE id=? AND status='QUEUED'",
                System.currentTimeMillis(),id)!=1)return;
        stream.publish(job(id,false));
        try {
            String payload=jdbc.queryForObject("SELECT payload_json FROM research_operation_jobs WHERE id=?",String.class,id);
            Submission job=json.readValue(payload,Submission.class);
            Map<String,Object> result;
            if("EXECUTION".equals(job.kind())){
                // The latest controller requires Analyze -> Review -> Execute once.
                // Queueing must NEVER bypass that human approval or send a legacy
                // execute request which could double-execute on a retry.
                jdbc.update("UPDATE research_operation_jobs SET status='REVIEW_REQUIRED',updated_at_ms=? WHERE id=?",
                        System.currentTimeMillis(),id);
                stream.publish(job(id,false));
                return;
            }
            ResearchExperimentController.Scenario scenario=json.readValue(json.writeValueAsString(job.scenario()),
                    ResearchExperimentController.Scenario.class);
            ResponseEntity<?> response=experiments.evaluate(scenario);
            if(!response.getStatusCode().is2xxSuccessful())
                throw new IllegalStateException("Estimator or Decision Engine rejected experiment (HTTP "+response.getStatusCode().value()+")");
            result=asMap(response.getBody());
            if(result.isEmpty())throw new IllegalStateException("Backend returned empty result");
            String jobStatus="SUCCEEDED"; // The experiment evaluation returned an HTTP 2xx real-engine result.
            Long reportId=null; // No inference was executed for an experiment.
            String output=json.writeValueAsString(result);
            if(output.length()>500_000)throw new IllegalStateException("Backend result exceeded queue storage limit");
            jdbc.update("UPDATE research_operation_jobs SET status=?,result_json=?,report_id=?,updated_at_ms=? WHERE id=?",
                    jobStatus,output,reportId,System.currentTimeMillis(),id);
        }catch(Exception exception){
            String message=exception.getClass().getSimpleName()+": "+String.valueOf(exception.getMessage());
            if(message.length()>900)message=message.substring(0,900);
            jdbc.update("UPDATE research_operation_jobs SET status='FAILED',error_text=?,updated_at_ms=? WHERE id=?",
                    message,System.currentTimeMillis(),id);
        }
        stream.publish(job(id,false));
    }

    /** Complete reviewed jobs only when the original orchestrator has saved their real execution. */
    public void reconcileReviewedExecutions() {
        List<Map<String,Object>> waiting=jdbc.queryForList(
                "SELECT id,task_id FROM research_operation_jobs WHERE status='REVIEW_REQUIRED' ORDER BY created_at_ms LIMIT 150");
        for(Map<String,Object> row:waiting){
            String id=String.valueOf(row.get("ID"));
            String task=String.valueOf(row.get("TASK_ID"));
            List<Map<String,Object>> reports=jdbc.queryForList(
                "SELECT id,status,executed_route FROM integrated_runs WHERE task_id=? AND executed_route IS NOT NULL ORDER BY id DESC LIMIT 1",task);
            if(reports.isEmpty())continue;
            Map<String,Object> report=reports.get(0);
            String state="SUCCESS".equals(report.get("STATUS"))?"SUCCEEDED":"FAILED";
            jdbc.update("UPDATE research_operation_jobs SET status=?,report_id=?,updated_at_ms=? WHERE id=? AND status='REVIEW_REQUIRED'",
                    state,report.get("ID"),System.currentTimeMillis(),id);
            stream.publish(job(id,false));
        }
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> asMap(Object val){
        return val instanceof Map<?,?> m?(Map<String,Object>)m:Map.of();
    }
}
