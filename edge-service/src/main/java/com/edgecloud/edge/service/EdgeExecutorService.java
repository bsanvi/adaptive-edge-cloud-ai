package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.ImageResizeResult;

import nu.pattern.OpenCV;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EdgeExecutorService {

    static {
        OpenCV.loadLocally();
    }

    public ImageResizeResult resizeImage(
            String inputPath,
            String outputPath,
            int width,
            int height
    ) {

        // Validate requested dimensions
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Width and height must be greater than 0"
            );
        }

        // Check input file
        File inputFile = new File(inputPath);

        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException(
                    "Input file does not exist: " + inputPath
            );
        }

        long inputSizeBytes = inputFile.length();

        // Read image
        Mat inputImage = Imgcodecs.imread(inputPath);

        if (inputImage.empty()) {
            inputImage.release();

            throw new IllegalArgumentException(
                    "Could not read image: " + inputPath
            );
        }

        int inputWidth = inputImage.width();
        int inputHeight = inputImage.height();

        // Make sure output directory exists
        File outputFile = new File(outputPath);
        File parentDirectory = outputFile.getParentFile();

        if (parentDirectory != null && !parentDirectory.exists()) {
            boolean created = parentDirectory.mkdirs();

            if (!created && !parentDirectory.exists()) {
                inputImage.release();

                throw new IllegalArgumentException(
                        "Could not create output directory"
                );
            }
        }

        Mat resizedImage = new Mat();

        try {

            // Start timing
            long startTime = System.nanoTime();

            // Resize image
            Imgproc.resize(
                    inputImage,
                    resizedImage,
                    new Size(width, height)
            );

            // Save resized image
            boolean saved = Imgcodecs.imwrite(
                    outputPath,
                    resizedImage
            );

            // Stop timing
            long endTime = System.nanoTime();

            if (!saved) {
                throw new IllegalArgumentException(
                        "Could not save image: " + outputPath
                );
            }

            double executionTimeMs =
                    (endTime - startTime) / 1_000_000.0;

            long outputSizeBytes = outputFile.length();

            double sizeReductionPercent =
                    inputSizeBytes == 0
                            ? 0.0
                            : ((double) (inputSizeBytes - outputSizeBytes)
                            / inputSizeBytes) * 100.0;

            return new ImageResizeResult(
                    "image_resize",
                    inputPath,
                    outputPath,
                    inputWidth,
                    inputHeight,
                    width,
                    height,
                    inputSizeBytes,
                    outputSizeBytes,
                    round(sizeReductionPercent, 2),
                    round(executionTimeMs, 3),
                    "success"
            );

        } finally {

            inputImage.release();
            resizedImage.release();
        }
    }

    private double round(double value, int decimals) {

        double factor = Math.pow(10, decimals);

        return Math.round(value * factor) / factor;
    }
}