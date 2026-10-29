package com.edgecloud.edge.dto;

public record MlInferenceResult(
        String task_type,
        String input_path,
        int predicted_class_index,
        String predicted_label,
        double confidence,
        double execution_time_ms,
        String status
) {
}