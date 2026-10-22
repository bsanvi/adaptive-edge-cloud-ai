package com.edgecloud.edge.dto;

public record ImageResizeResult(
        String task_type,
        String input_path,
        String output_path,
        int input_width,
        int input_height,
        int output_width,
        int output_height,
        long input_size_bytes,
        long output_size_bytes,
        double size_reduction_percent,
        double execution_time_ms,
        String status
) {
}