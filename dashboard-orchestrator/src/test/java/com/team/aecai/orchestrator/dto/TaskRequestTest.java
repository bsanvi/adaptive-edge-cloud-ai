
package com.team.aecai.orchestrator.dto;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskRequestTest {

    @Test
    void testTaskRequestCreation() {

        TaskRequest task = new TaskRequest(
            "TASK-001",
            List.of(5.1, 3.5, 1.4, 0.2),
            500,
            "NORMAL",
            "BALANCED",
            "MEDIUM"
        );

        assertEquals("TASK-001", task.taskId());
        assertEquals(4, task.features().size());
        assertEquals(500.0, task.deadlineMs());
        assertEquals("NORMAL", task.privacyLevel());
        assertEquals("BALANCED", task.priorityProfile());
        assertEquals("MEDIUM", task.requestedComplexity());
    }
}
