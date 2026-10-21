package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.ImageResizeResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EdgeExecutorServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldResizeImageSuccessfully() throws Exception {

        // Create a temporary 1200 x 800 image
        File inputFile = tempDir.resolve("input.jpg").toFile();

        BufferedImage image =
                new BufferedImage(
                        1200,
                        800,
                        BufferedImage.TYPE_INT_RGB
                );

        ImageIO.write(image, "jpg", inputFile);

        File outputFile =
                tempDir.resolve("output.jpg").toFile();

        EdgeExecutorService executor =
                new EdgeExecutorService();

        ImageResizeResult result =
                executor.resizeImage(
                        inputFile.getAbsolutePath(),
                        outputFile.getAbsolutePath(),
                        640,
                        480
                );

        assertEquals("image_resize", result.task_type());

        assertEquals(1200, result.input_width());
        assertEquals(800, result.input_height());

        assertEquals(640, result.output_width());
        assertEquals(480, result.output_height());

        assertTrue(result.input_size_bytes() > 0);
        assertTrue(result.output_size_bytes() > 0);

        assertTrue(result.execution_time_ms() >= 0);

        assertEquals("success", result.status());

        assertTrue(outputFile.exists());
    }
}