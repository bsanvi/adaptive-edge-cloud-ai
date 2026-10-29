package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.EdgeMetrics;
import com.edgecloud.edge.dto.EdgeTaskRequest;
import com.edgecloud.edge.dto.EdgeTaskResponse;
import com.edgecloud.edge.dto.ImageResizeResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdgeTaskServiceTest {

    @Test
    void shouldReturnSuccessfulTaskResponse() {

        SystemMonitorService monitorService =
                mock(SystemMonitorService.class);

        EdgeExecutorService executorService =
                mock(EdgeExecutorService.class);

        EdgeMetrics metricsBefore =
                new EdgeMetrics(
                        10.0,
                        60.0,
                        9000,
                        7000,
                        80.0,
                        true,
                        "before"
                );

        EdgeMetrics metricsAfter =
                new EdgeMetrics(
                        20.0,
                        61.0,
                        9100,
                        6900,
                        80.0,
                        true,
                        "after"
                );

        when(monitorService.getEdgeMetrics())
                .thenReturn(metricsBefore, metricsAfter);

        ImageResizeResult resizeResult =
                new ImageResizeResult(
                        "image_resize",
                        "input.jpg",
                        "output.jpg",
                        1200,
                        800,
                        640,
                        480,
                        15000,
                        5000,
                        66.67,
                        8.5,
                        "success"
                );

        when(
                executorService.resizeImage(
                        "input.jpg",
                        "output.jpg",
                        640,
                        480
                )
        ).thenReturn(resizeResult);

        EdgeTaskService taskService =
                new EdgeTaskService(
                        monitorService,
                        executorService
                );

        EdgeTaskRequest request =
                new EdgeTaskRequest(
                        "task_001",
                        "input.jpg",
                        "output.jpg",
                        640,
                        480
                );

        EdgeTaskResponse response =
                taskService.runEdgeTask(request);

        assertEquals("task_001", response.task_id());
        assertEquals("edge", response.execution_location());
        assertEquals("success", response.status());
        assertEquals("image_resize", response.task_type());

        assertEquals(8.5, response.execution_time_ms());

        assertEquals(metricsBefore, response.metrics_before());
        assertEquals(metricsAfter, response.metrics_after());

        assertEquals(resizeResult, response.result());

        assertNull(response.error());
    }

    @Test
    void shouldReturnFailedTaskResponseWhenExecutionFails() {

        SystemMonitorService monitorService =
                mock(SystemMonitorService.class);

        EdgeExecutorService executorService =
                mock(EdgeExecutorService.class);

        EdgeMetrics metricsBefore =
                new EdgeMetrics(
                        10.0,
                        60.0,
                        9000,
                        7000,
                        80.0,
                        false,
                        "before"
                );

        EdgeMetrics metricsAfter =
                new EdgeMetrics(
                        11.0,
                        60.5,
                        9050,
                        6950,
                        80.0,
                        false,
                        "after"
                );

        when(monitorService.getEdgeMetrics())
                .thenReturn(metricsBefore, metricsAfter);

        when(
                executorService.resizeImage(
                        "missing.jpg",
                        "output.jpg",
                        640,
                        480
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "Input file does not exist: missing.jpg"
                )
        );

        EdgeTaskService taskService =
                new EdgeTaskService(
                        monitorService,
                        executorService
                );

        EdgeTaskRequest request =
                new EdgeTaskRequest(
                        "task_002",
                        "missing.jpg",
                        "output.jpg",
                        640,
                        480
                );

        EdgeTaskResponse response =
                taskService.runEdgeTask(request);

        assertEquals("task_002", response.task_id());
        assertEquals("edge", response.execution_location());
        assertEquals("failed", response.status());
        assertEquals("image_resize", response.task_type());

        assertNull(response.execution_time_ms());
        assertNull(response.result());

        assertNotNull(response.error());
        assertTrue(
                response.error()
                        .contains("Input file does not exist")
        );

        assertEquals(metricsBefore, response.metrics_before());
        assertEquals(metricsAfter, response.metrics_after());
    }
}
