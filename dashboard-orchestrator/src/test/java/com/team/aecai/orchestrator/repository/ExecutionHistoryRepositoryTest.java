
package com.team.aecai.orchestrator.repository;

import com.team.aecai.orchestrator.dto.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionHistoryRepositoryTest {

    @Test
    void shouldSaveAndRetrieveExecution() {

        DriverManagerDataSource datasource =
                new DriverManagerDataSource();

        datasource.setDriverClassName("org.h2.Driver");
        datasource.setUrl(
                "jdbc:h2:mem:historytest;DB_CLOSE_DELAY=-1"
        );
        datasource.setUsername("sa");
        datasource.setPassword("");

        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(
                        new ClassPathResource("schema.sql")
                );

        populator.execute(datasource);

        JdbcTemplate jdbc = new JdbcTemplate(datasource);

        ExecutionHistoryRepository repository =
                new ExecutionHistoryRepository(jdbc);

        OrchestrationResult result =
                new OrchestrationResult(
                        "TASK-DB-001",
                        "SUCCESS",
                        "CLOUD",
                        "EDGE",
                        true,
                        new InferenceResponse(
                                "TASK-DB-001",
                                "SETOSA",
                                0.94,
                                12.0
                        ),
                        null,
                        45.0,
                        "Cloud failed; executed on Edge"
                );

        repository.save(result);

        List<ExecutionHistoryEntry> entries =
                repository.findRecent(10);

        assertEquals(1, entries.size());

        ExecutionHistoryEntry saved = entries.get(0);

        assertEquals("TASK-DB-001", saved.taskId());
        assertEquals("SUCCESS", saved.status());
        assertEquals("CLOUD", saved.selectedRoute());
        assertEquals("EDGE", saved.actualRoute());
        assertTrue(saved.fallbackUsed());
        assertEquals("SETOSA", saved.prediction());
        assertEquals(0.94, saved.confidence());
        assertEquals(45.0, saved.totalTimeMs());
        assertNotNull(saved.createdAt());
    }
}
