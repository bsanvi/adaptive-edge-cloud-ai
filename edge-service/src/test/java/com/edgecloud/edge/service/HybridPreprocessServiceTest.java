package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.HybridPreprocessRequest;
import com.edgecloud.edge.dto.HybridPreprocessResult;
import com.edgecloud.edge.dto.ImageResizeResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HybridPreprocessServiceTest {

    @Test
    void shouldPreprocessImageForHybridExecution() {

        EdgeExecutorService edgeExecutorService =
                mock(EdgeExecutorService.class);

        HybridPreprocessService service =
                new HybridPreprocessService(
                        edgeExecutorService
                );

        HybridPreprocessRequest request =
                new HybridPreprocessRequest(
                        "TASK-101",
                        "input.jpg",
                        "processed.jpg"
                );

        ImageResizeResult resizeResult =
                new ImageResizeResult(
                        "image_resize",
                        "input.jpg",
                        "processed.jpg",
                        1200,
                        800,
                        224,
                        224,
                        15000,
                        3000,
                        80.0,
                        5.25,
                        "success"
                );

        when(
                edgeExecutorService.resizeImage(
                        "input.jpg",
                        "processed.jpg",
                        224,
                        224
                )
        ).thenReturn(resizeResult);

        HybridPreprocessResult result =
                service.preprocess(request);

        assertEquals(
                "TASK-101",
                result.task_id()
        );

        assertEquals(
                "hybrid_preprocess",
                result.task_type()
        );

        assertEquals(
                "input.jpg",
                result.input_path()
        );

        assertEquals(
                "processed.jpg",
                result.processed_path()
        );

        assertEquals(
                1200,
                result.original_width()
        );

        assertEquals(
                800,
                result.original_height()
        );

        assertEquals(
                224,
                result.processed_width()
        );

        assertEquals(
                224,
                result.processed_height()
        );

        assertEquals(
                15000,
                result.original_size_bytes()
        );

        assertEquals(
                3000,
                result.processed_size_bytes()
        );

        assertEquals(
                80.0,
                result.size_reduction_percent()
        );

        assertEquals(
                5.25,
                result.processing_time_ms()
        );

        assertEquals(
                "success",
                result.status()
        );

        verify(
                edgeExecutorService,
                times(1)
        ).resizeImage(
                "input.jpg",
                "processed.jpg",
                224,
                224
        );
    }
}
