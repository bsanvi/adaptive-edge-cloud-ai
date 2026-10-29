package com.edgecloud.edge.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.edgecloud.edge.dto.MlInferenceResult;

class MlInferenceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRunImageClassification() throws Exception {

        // Create a temporary test image
        File inputFile =
                tempDir.resolve("test-image.jpg").toFile();

        BufferedImage image =
                new BufferedImage(
                        300,
                        300,
                        BufferedImage.TYPE_INT_RGB
                );

        ImageIO.write(
                image,
                "jpg",
                inputFile
        );

        MlInferenceService service =
                new MlInferenceService();

        MlInferenceResult result =
                service.classifyImage(
                        inputFile.getAbsolutePath()
                );

        assertEquals(
                "image_classification",
                result.task_type()
        );

        assertEquals(
                "success",
                result.status()
        );

        assertTrue(
                result.predicted_class_index() >= 0
        );

        assertTrue(
                result.predicted_class_index() < 1000
        );

        assertNotNull(
                result.predicted_label()
        );

        assertFalse(
                result.predicted_label().isBlank()
        );

        assertTrue(
                result.confidence() >= 0.0
        );

        assertTrue(
                result.confidence() <= 1.0
        );

        assertTrue(
                result.execution_time_ms() >= 0
        );
    }

    @Test
    void shouldRejectMissingImage() {

        MlInferenceService service =
                new MlInferenceService();

        String missingPath =
                tempDir.resolve("missing.jpg").toString();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.classifyImage(
                                missingPath
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("Input file does not exist")
        );
    }
}