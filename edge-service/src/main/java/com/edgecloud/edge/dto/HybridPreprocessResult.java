package com.edgecloud.edge.dto;

public record HybridPreprocessResult(
        String task_id,
        String task_type,
        String input_path,
        String processed_path,
        int original_width,
        int original_height,
        int processed_width,
        int processed_height,
        long original_size_bytes,
        long processed_size_bytes,
        double size_reduction_percent,
        double processing_time_ms,
        String status
) {
}
