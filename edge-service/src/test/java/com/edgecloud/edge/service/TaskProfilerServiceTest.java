package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.TaskProfileRequest;
import com.edgecloud.edge.dto.TaskProfileResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TaskProfilerServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldClassifySmallImageAsLowComplexity()
            throws Exception {

        File imageFile =
                createImage(
                        "small.jpg",
                        200,
                        200
                );

        TaskProfilerService service =
                new TaskProfilerService();

        TaskProfileRequest request =
                new TaskProfileRequest(
                        "TASK-LOW",
                        imageFile.getAbsolutePath(),
                        500.0,
                        "NORMAL",
                        "BALANCED"
                );

        TaskProfileResult result =
                service.profileTask(request);

        assertEquals(
                "TASK-LOW",
                result.task_id()
        );

        assertEquals(
                "image_classification",
                result.task_type()
        );

        assertEquals(
                200,
                result.image_width()
        );

        assertEquals(
                200,
                result.image_height()
        );

        assertEquals(
                40_000,
                result.total_pixels()
        );

        assertEquals(
                "LOW",
                result.complexity()
        );

        assertEquals(
                "NORMAL",
                result.privacy_level()
        );

        assertEquals(
                "BALANCED",
                result.priority_profile()
        );

        assertEquals(
                "success",
                result.status()
        );
    }

    @Test
    void shouldClassifyMediumImageAsMediumComplexity()
            throws Exception {

        File imageFile =
                createImage(
                        "medium.jpg",
                        800,
                        600
                );

        TaskProfilerService service =
                new TaskProfilerService();

        TaskProfileRequest request =
                new TaskProfileRequest(
                        "TASK-MEDIUM",
                        imageFile.getAbsolutePath(),
                        300.0,
                        "NORMAL",
                        "REAL_TIME"
                );

        TaskProfileResult result =
                service.profileTask(request);

        assertEquals(
                480_000,
                result.total_pixels()
        );

        assertEquals(
                "MEDIUM",
                result.complexity()
        );

        assertEquals(
                300.0,
                result.deadline_ms()
        );

        assertEquals(
                "REAL_TIME",
                result.priority_profile()
        );
    }

    @Test
    void shouldClassifyLargeImageAsHighComplexity()
            throws Exception {

        File imageFile =
                createImage(
                        "large.jpg",
                        2000,
                        1200
                );

        TaskProfilerService service =
                new TaskProfilerService();

        TaskProfileRequest request =
                new TaskProfileRequest(
                        "TASK-HIGH",
                        imageFile.getAbsolutePath(),
                        1000.0,
                        "SENSITIVE",
                        "BATTERY_SAVER"
                );

        TaskProfileResult result =
                service.profileTask(request);

        assertEquals(
                2_400_000,
                result.total_pixels()
        );

        assertEquals(
                "HIGH",
                result.complexity()
        );

        assertEquals(
                "SENSITIVE",
                result.privacy_level()
        );

        assertEquals(
                "BATTERY_SAVER",
                result.priority_profile()
        );
    }

    @Test
    void shouldRejectMissingInputFile() {

        TaskProfilerService service =
                new TaskProfilerService();

        String missingPath =
                tempDir.resolve(
                        "missing.jpg"
                ).toString();

        TaskProfileRequest request =
                new TaskProfileRequest(
                        "TASK-MISSING",
                        missingPath,
                        500.0,
                        "NORMAL",
                        "BALANCED"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.profileTask(request)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Input file does not exist"
                        )
        );
    }

    private File createImage(
            String filename,
            int width,
            int height
    ) throws Exception {

        File file =
                tempDir.resolve(
                        filename
                ).toFile();

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        ImageIO.write(
                image,
                "jpg",
                file
        );

        return file;
    }
}
