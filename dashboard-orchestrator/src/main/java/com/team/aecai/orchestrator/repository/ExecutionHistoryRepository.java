
package com.team.aecai.orchestrator.repository;

import com.team.aecai.orchestrator.dto.ExecutionHistoryEntry;
import com.team.aecai.orchestrator.dto.InferenceResponse;
import com.team.aecai.orchestrator.dto.OrchestrationResult;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ExecutionHistoryRepository {

    private final JdbcTemplate jdbc;

    public ExecutionHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(OrchestrationResult result) {

        InferenceResponse prediction = result.prediction();

        String predictionLabel = prediction == null
                ? null
                : prediction.prediction();

        Double confidence = prediction == null
                ? null
                : prediction.confidence();

        String sql = """
            INSERT INTO execution_history (
                task_id, status, selected_route,
                actual_route, fallback_used,
                prediction, confidence,
                total_time_ms, message, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbc.update(
                sql,
                result.taskId(),
                result.status(),
                result.selectedRoute(),
                result.actualRoute(),
                result.fallbackUsed(),
                predictionLabel,
                confidence,
                result.totalTimeMs(),
                result.message(),
                Instant.now().toString()
        );
    }

    public List<ExecutionHistoryEntry> findRecent(int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 100));

        String sql = """
            SELECT id, task_id, status, selected_route,
                   actual_route, fallback_used,
                   prediction, confidence,
                   total_time_ms, message, created_at
            FROM execution_history
            ORDER BY id DESC
            LIMIT ?
            """;

        return jdbc.query(
                sql,
                (rs, rowNum) ->
                        new ExecutionHistoryEntry(
                                rs.getLong("id"),
                                rs.getString("task_id"),
                                rs.getString("status"),
                                rs.getString("selected_route"),
                                rs.getString("actual_route"),
                                rs.getBoolean("fallback_used"),
                                rs.getString("prediction"),
                                rs.getObject(
                                        "confidence",
                                        Double.class
                                ),
                                rs.getDouble("total_time_ms"),
                                rs.getString("message"),
                                rs.getString("created_at")
                        ),
                safeLimit
        );
    }
}
