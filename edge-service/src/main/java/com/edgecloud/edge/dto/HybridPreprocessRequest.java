package com.edgecloud.edge.dto;

public record HybridPreprocessRequest(
        String task_id,
        String input_path,
        String output_path
) {
}
