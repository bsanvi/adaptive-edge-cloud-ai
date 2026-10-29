package com.edgecloud.edge.dto;

public record TaskProfileResult(
        String task_id,
        String task_type,
        String input_path,
        long input_data_size_bytes,
        int image_width,
        int image_height,
        long total_pixels,
        String complexity,
        double deadline_ms,
        String privacy_level,
        String priority_profile,
        String status
) {
}
