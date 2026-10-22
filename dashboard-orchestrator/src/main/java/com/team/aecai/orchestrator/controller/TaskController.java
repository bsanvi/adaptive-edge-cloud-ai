
package com.team.aecai.orchestrator.controller;

import com.team.aecai.orchestrator.dto.TaskRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @PostMapping("/echo")
    public Map<String, Object> echoTask(
            @RequestBody TaskRequest task) {

        return Map.of(
            "message", "Task received successfully",
            "taskId", task.taskId(),
            "featureCount", task.features().size(),
            "deadlineMs", task.deadlineMs(),
            "privacyLevel", task.privacyLevel()
        );
    }
}
