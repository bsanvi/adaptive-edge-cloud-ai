package com.edgecloud.edge.dto;

public record HybridPostprocessRequest(
        String task_id,
        int class_index,
        double confidence
) {
}
