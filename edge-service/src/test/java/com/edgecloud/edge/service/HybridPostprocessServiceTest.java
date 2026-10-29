package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.HybridPostprocessRequest;
import com.edgecloud.edge.dto.HybridPostprocessResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HybridPostprocessServiceTest {

    @Test
    void shouldConvertClassIndexToLabel() {

        HybridPostprocessService service =
                new HybridPostprocessService();

        HybridPostprocessRequest request =
                new HybridPostprocessRequest(
                        "TASK-101",
                        281,
                        0.82
                );

        HybridPostprocessResult result =
                service.postprocess(request);

        assertEquals(
                "TASK-101",
                result.task_id()
        );

        assertEquals(
                "hybrid_postprocess",
                result.task_type()
        );

        assertEquals(
                281,
                result.class_index()
        );

        assertNotNull(
                result.predicted_label()
        );

        assertFalse(
                result.predicted_label().isBlank()
        );

        assertEquals(
                0.82,
                result.confidence()
        );

        assertTrue(
                result.processing_time_ms() >= 0
        );

        assertEquals(
                "success",
                result.status()
        );
    }

    @Test
    void shouldRejectInvalidClassIndex() {

        HybridPostprocessService service =
                new HybridPostprocessService();

        HybridPostprocessRequest request =
                new HybridPostprocessRequest(
                        "TASK-102",
                        1500,
                        0.50
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.postprocess(request)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Invalid class index")
        );
    }
}
