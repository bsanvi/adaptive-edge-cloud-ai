
package com.team.aecai.orchestrator.evaluation;

import com.team.aecai.orchestrator.dto.*;
import com.team.aecai.orchestrator.repository.ExecutionHistoryRepository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationServiceTest {

    @Test
    void shouldCalculateMetricsFromStoredExecutions() {

        DriverManagerDataSource datasource =
                new DriverManagerDataSource();

        datasource.setDriverClassName("org.h2.Driver");
        
        datasource.setUrl(
                "jdbc:h2:mem:test_" +
                UUID.randomUUID().toString().replace("-", "") +
              ";DB_CLOSE_DELAY=-1"
        );

        
        datasource.setUsername("sa");
        datasource.setPassword("");

        new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql")
        ).execute(datasource);

        JdbcTemplate jdbc = new JdbcTemplate(datasource);

        ExecutionHistoryRepository history =
                new ExecutionHistoryRepository(jdbc);

        history.save(new OrchestrationResult(
                "TASK-001",
                "SUCCESS",
                "EDGE",
                "EDGE",
                false,
                new InferenceResponse(
                        "TASK-001", "SETOSA", 0.95, 10
                ),
                null,
                40.0,
                "Success"
        ));

        history.save(new OrchestrationResult(
                "TASK-002",
                "SUCCESS",
                "CLOUD",
                "EDGE",
                true,
                new InferenceResponse(
                        "TASK-002", "SETOSA", 0.95, 12
                ),
                null,
                60.0,
                "Fallback"
        ));

        history.save(new OrchestrationResult(
                "TASK-003",
                "FAILED",
                null,
                null,
                false,
                null,
                null,
                20.0,
                "Failure"
        ));

        EvaluationService service =
                new EvaluationService(jdbc);

        EvaluationSummary summary = service.getSummary();

        assertEquals(3, summary.totalExecutions());
        assertEquals(2, summary.successfulExecutions());
        assertEquals(1, summary.failedExecutions());
        assertEquals(1, summary.fallbackCount());

        assertEquals(
                2L,
                summary.routeCounts().get("EDGE")
        );

        assertEquals(
                50.0,
                summary.averageExecutionTimeMs()
        );

        assertEquals(
                50.0,
                summary.averageTimeByRouteMs().get("EDGE")
        );

        assertEquals(
                2.0 / 3.0 * 100.0,
                summary.successRatePercent(),
                0.0001
        );
    }
}
