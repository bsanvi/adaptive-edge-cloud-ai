package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.HybridPreprocessRequest;
import com.edgecloud.edge.dto.HybridPreprocessResult;
import com.edgecloud.edge.dto.ImageResizeResult;

import org.springframework.stereotype.Service;

@Service
public class HybridPreprocessService {

    private static final int MODEL_WIDTH = 224;
    private static final int MODEL_HEIGHT = 224;

    private final EdgeExecutorService edgeExecutorService;

    public HybridPreprocessService(
            EdgeExecutorService edgeExecutorService
    ) {
        this.edgeExecutorService = edgeExecutorService;
    }

    public HybridPreprocessResult preprocess(
            HybridPreprocessRequest request
    ) {

        ImageResizeResult resizeResult =
                edgeExecutorService.resizeImage(
                        request.input_path(),
                        request.output_path(),
                        MODEL_WIDTH,
                        MODEL_HEIGHT
                );

        return new HybridPreprocessResult(
                request.task_id(),
                "hybrid_preprocess",
                request.input_path(),
                request.output_path(),
                resizeResult.input_width(),
                resizeResult.input_height(),
                resizeResult.output_width(),
                resizeResult.output_height(),
                resizeResult.input_size_bytes(),
                resizeResult.output_size_bytes(),
                resizeResult.size_reduction_percent(),
                resizeResult.execution_time_ms(),
                "success"
        );
    }
}
