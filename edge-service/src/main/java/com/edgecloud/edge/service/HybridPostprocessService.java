package com.edgecloud.edge.service;

import com.edgecloud.edge.dto.HybridPostprocessRequest;
import com.edgecloud.edge.dto.HybridPostprocessResult;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class HybridPostprocessService {

    private static final String LABEL_PATH =
            "models/imagenet_classes.txt";

    private final List<String> labels;

    public HybridPostprocessService() {

        try {
            labels =
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

    public HybridPostprocessResult postprocess(
            HybridPostprocessRequest request
    ) {

        long startTime =
                System.nanoTime();

        int classIndex =
                request.class_index();

        if (
                classIndex < 0
                        || classIndex >= labels.size()
        ) {
            throw new IllegalArgumentException(
                    "Invalid class index: "
                            + classIndex
            );
        }

        String predictedLabel =
                labels.get(classIndex);

        long endTime =
                System.nanoTime();

        double processingTimeMs =
                (endTime - startTime)
                        / 1_000_000.0;

        return new HybridPostprocessResult(
                request.task_id(),
                "hybrid_postprocess",
                classIndex,
                predictedLabel,
                round(
                        request.confidence(),
                        4
                ),
                round(
                        processingTimeMs,
                        3
                ),
                "success"
        );
    }

    private double round(
            double value,
            int decimals
    ) {

        double factor =
                Math.pow(
                        10,
                        decimals
                );

        return Math.round(
                value * factor
        ) / factor;
    }
}
