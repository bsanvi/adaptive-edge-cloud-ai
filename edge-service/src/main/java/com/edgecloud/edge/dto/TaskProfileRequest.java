package com.edgecloud.edge.dto;

public record TaskProfileRequest(
        String task_id,
        String input_path,
        double deadline_ms,
        String privacy_level,
        String priority_profile
) {
}
