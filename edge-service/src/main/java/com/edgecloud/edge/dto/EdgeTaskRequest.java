package com.edgecloud.edge.dto;

public record EdgeTaskRequest(
        String task_id,
        String input_path,
        String output_path,
        int width,
        int height
) {
}