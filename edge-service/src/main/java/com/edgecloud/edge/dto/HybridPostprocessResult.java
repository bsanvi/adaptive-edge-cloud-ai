package com.edgecloud.edge.dto;

public record HybridPostprocessResult(
        String task_id,
        String task_type,
        int class_index,
        String predicted_label,
        double confidence,
        double processing_time_ms,
        String status
) {
}
