package com.edgecloud.edge.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import com.edgecloud.edge.dto.MlInferenceResult;

import nu.pattern.OpenCV;

@Service
public class MlInferenceService {

    private static final String MODEL_PATH =
            "models/squeezenet1.1-7.onnx";

    private static final String LABEL_PATH =
            "models/imagenet_classes.txt";

    private final Net net;
    private final List<String> labels;

    static {
        OpenCV.loadLocally();
    }

    public MlInferenceService() {

        this.net = Dnn.readNetFromONNX(MODEL_PATH);

        if (this.net.empty()) {
            throw new IllegalStateException(
                    "Could not load ML model: " + MODEL_PATH
            );
        }

        try {
            this.labels =
                    Files.readAllLines(
                            Path.of(LABEL_PATH)
                    );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load ImageNet labels",
                    exception
            );
        }

        if (labels.size() != 1000) {
            throw new IllegalStateException(
                    "Expected 1000 ImageNet labels but found "
                            + labels.size()
            );
        }
    }

    public MlInferenceResult classifyImage(
            String inputPath
    ) {

        Path imagePath = Path.of(inputPath);

        if (!Files.exists(imagePath)) {
            throw new IllegalArgumentException(
                    "Input file does not exist: " + inputPath
            );
        }

        Mat image =
                Imgcodecs.imread(inputPath);

        if (image.empty()) {
            image.release();

            throw new IllegalArgumentException(
                    "Could not read image: " + inputPath
            );
        }

        Mat resized = new Mat();
        Mat rgb = new Mat();
        Mat normalized = new Mat();
        Mat blob = null;
        Mat output = null;

        try {

            // Resize to model input size
            Imgproc.resize(
                    image,
                    resized,
                    new Size(224, 224)
            );

            // Convert BGR -> RGB
            Imgproc.cvtColor(
                    resized,
                    rgb,
                    Imgproc.COLOR_BGR2RGB
            );

            // Convert pixel range from 0-255 to 0-1
            rgb.convertTo(
                    normalized,
                    CvType.CV_32FC3,
                    1.0 / 255.0
            );

            // Split RGB channels
            List<Mat> channels =
                    new ArrayList<>();

            Core.split(normalized, channels);

            // ImageNet normalization
            normalizeChannel(
                    channels.get(0),
                    0.485,
                    0.229
            );

            normalizeChannel(
                    channels.get(1),
                    0.456,
                    0.224
            );

            normalizeChannel(
                    channels.get(2),
                    0.406,
                    0.225
            );

            Core.merge(
                    channels,
                    normalized
            );

            for (Mat channel : channels) {
                channel.release();
            }

            // Convert image into neural-network tensor
            blob =
                    Dnn.blobFromImage(
                            normalized
                    );

            long startTime =
                    System.nanoTime();

            net.setInput(blob);

            output =
                    net.forward();

            long endTime =
                    System.nanoTime();

            double executionTimeMs =
                    (endTime - startTime)
                            / 1_000_000.0;

            Mat scores =
                    output.reshape(1, 1);

            int numberOfClasses =
                    (int) scores.total();

            double[] logits =
                    new double[numberOfClasses];

            for (int i = 0; i < numberOfClasses; i++) {

                double[] value =
                        scores.get(0, i);

                logits[i] =
                        value[0];
            }

            int predictedIndex =
                    findMaximumIndex(logits);

            double confidence =
                    calculateSoftmaxConfidence(
                            logits,
                            predictedIndex
                    );

            String predictedLabel =
                    labels.get(predictedIndex);

            return new MlInferenceResult(
                    "image_classification",
                    inputPath,
                    predictedIndex,
                    predictedLabel,
                    round(confidence, 4),
                    round(executionTimeMs, 3),
                    "success"
            );

        } finally {

            image.release();
            resized.release();
            rgb.release();
            normalized.release();

            if (blob != null) {
                blob.release();
            }

            if (output != null) {
                output.release();
            }
        }
    }

    private void normalizeChannel(
            Mat channel,
            double mean,
            double std
    ) {

        Core.subtract(
                channel,
                new Scalar(mean),
                channel
        );

        Core.divide(
                channel,
                new Scalar(std),
                channel
        );
    }

    private int findMaximumIndex(
            double[] values
    ) {

        int maxIndex = 0;

        for (int i = 1; i < values.length; i++) {

            if (values[i] > values[maxIndex]) {
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    private double calculateSoftmaxConfidence(
            double[] logits,
            int predictedIndex
    ) {

        double max =
                logits[predictedIndex];

        double sum = 0.0;

        for (double logit : logits) {
            sum += Math.exp(logit - max);
        }

        return 1.0 / sum;
    }

    private double round(
            double value,
            int decimals
    ) {

        double factor =
                Math.pow(10, decimals);

        return Math.round(
                value * factor
        ) / factor;
    }
}