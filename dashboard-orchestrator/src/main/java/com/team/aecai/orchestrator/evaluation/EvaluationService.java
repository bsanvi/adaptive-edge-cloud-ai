
package com.team.aecai.orchestrator.evaluation;

import com.team.aecai.orchestrator.dto.EvaluationSummary;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EvaluationService {

    private final JdbcTemplate jdbc;

    public EvaluationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public EvaluationSummary getSummary() {

        String sql = """
            SELECT status, actual_route,
                   fallback_used, total_time_ms
            FROM execution_history
            """;

        long[] totals = new long[5];
        double[] successfulTime = new double[1];

        Map<String, Long> routeCounts = new HashMap<>();
        Map<String, Double> routeTimeTotals = new HashMap<>();

        jdbc.query(sql, rs -> {

            String status = rs.getString("status");
            String route = rs.getString("actual_route");
            boolean fallback = rs.getBoolean("fallback_used");
            double time = rs.getDouble("total_time_ms");

            totals[0]++;

            if ("SUCCESS".equals(status)) {
                totals[1]++;
                successfulTime[0] += time;

                if (route != null) {
                    routeCounts.merge(route, 1L, Long::sum);
                    routeTimeTotals.merge(
                            route, time, Double::sum
                    );
                }

            } else if ("FAILED".equals(status)) {
                totals[2]++;

            } else if ("NO_FEASIBLE_ROUTE".equals(status)) {
                totals[3]++;
            }

            if (fallback) {
                totals[4]++;
            }
        });

        Map<String, Double> averages = new HashMap<>();

        for (String route : routeCounts.keySet()) {
            averages.put(
                    route,
                    routeTimeTotals.get(route)
                            / routeCounts.get(route)
            );
        }

        double successRate = totals[0] == 0
                ? 0.0
                : totals[1] * 100.0 / totals[0];

        double averageTime = totals[1] == 0
                ? 0.0
                : successfulTime[0] / totals[1];

        return new EvaluationSummary(
                totals[0],
                totals[1],
                totals[2],
                totals[3],
                totals[4],
                successRate,
                averageTime,
                Map.copyOf(routeCounts),
                Map.copyOf(averages)
        );
    }
}
