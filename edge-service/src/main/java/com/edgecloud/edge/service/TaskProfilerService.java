package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.TaskProfileRequest;
import com.edgecloud.edge.dto.TaskProfileResult;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TaskProfilerService {

    public TaskProfileResult profileTask(
            TaskProfileRequest request
    ) {

        Path inputPath =
                Path.of(request.input_path());

        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException(
                    "Input file does not exist: "
                            + request.input_path()
            );
        }

        if (!Files.isRegularFile(inputPath)) {
            throw new IllegalArgumentException(
                    "Input path is not a file: "
                            + request.input_path()
            );
        }

        try {

            long inputSizeBytes =
                    Files.size(inputPath);

            BufferedImage image =
                    ImageIO.read(
                            inputPath.toFile()
                    );

            if (image == null) {
                throw new IllegalArgumentException(
                        "Could not read image: "
                                + request.input_path()
                );
            }

            int width =
                    image.getWidth();

            int height =
                    image.getHeight();

            long totalPixels =
                    (long) width * height;

            String complexity =
                    determineComplexity(
                            totalPixels,
                            inputSizeBytes
                    );

            return new TaskProfileResult(
                    request.task_id(),
                    "image_classification",
                    request.input_path(),
                    inputSizeBytes,
                    width,
                    height,
                    totalPixels,
                    complexity,
                    request.deadline_ms(),
                    request.privacy_level(),
                    request.priority_profile(),
                    "success"
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to profile input file",
                    exception
            );
        }
    }

    private String determineComplexity(
            long totalPixels,
            long inputSizeBytes
    ) {

        long oneMb =
                1024L * 1024L;

        long fiveMb =
                5L * 1024L * 1024L;

        if (
                totalPixels <= 250_000
                        && inputSizeBytes <= oneMb
        ) {
            return "LOW";
        }

        if (
                totalPixels <= 2_000_000
                        && inputSizeBytes <= fiveMb
        ) {
            return "MEDIUM";
        }

        return "HIGH";
    }
}
