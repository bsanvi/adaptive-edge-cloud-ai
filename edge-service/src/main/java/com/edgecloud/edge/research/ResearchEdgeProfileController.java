package com.edgecloud.edge.research;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;
import java.util.List;

import static com.edgecloud.edge.research.Research.ImageInput;
import static com.edgecloud.edge.research.Research.ImageProfile;
import static com.edgecloud.edge.research.Research.Profile;

@RestController
@RequestMapping("/api/v2/edge")
public class ResearchEdgeProfileController {

    private static final long LOW_PIXEL_LIMIT = 250_000L;
    private static final long MEDIUM_PIXEL_LIMIT = 2_000_000L;

    private final ImageModel imageModel;

    public ResearchEdgeProfileController(
            ImageModel imageModel
    ) {
        this.imageModel = imageModel;
    }

    @PostMapping("/profile")
    public ImageProfile profile(
            @RequestBody ImageInput request
    ) {

        long start = System.nanoTime();

        /*
         * Validation is handled by the shared v2 ImageModel.
         *
         * It verifies:
         * - requestId
         * - privacy value
         * - model version
         * - Base64 image
         * - image size
         */
        byte[] originalBytes =
                imageModel.validate(request);

        ImageModel.Decoded decoded =
                imageModel.decode(originalBytes);

        BufferedImage image =
                decoded.image();

        int width = image.getWidth();
        int height = image.getHeight();

        long pixelCount =
                (long) width * height;

        String complexity =
                calculateComplexity(pixelCount);

        /*
         * The v2 Hybrid contract sends a lossless
         * 224 x 224 PNG from Edge to Cloud.
         *
         * We generate it here only to determine the
         * exact Hybrid payload size for profiling.
         */
        BufferedImage resized =
                imageModel.resize(image);

        byte[] hybridPng =
                imageModel.png(resized);

        Profile profile =
                new Profile(
                        originalBytes.length,
                        width,
                        height,
                        pixelCount,
                        decoded.format(),
                        complexity,
                        "pixel-count-v2",
                        ImageModel.VERSION,
                        List.of(
                                1,
                                3,
                                224,
                                224
                        ),
                        ImageModel.PREPROCESS,
                        hybridPng.length
                );

        double profilingMs =
                (System.nanoTime() - start)
                        / 1_000_000.0;

        return new ImageProfile(
                profile,
                profilingMs
        );
    }

    private String calculateComplexity(
            long pixelCount
    ) {

        if (pixelCount <= LOW_PIXEL_LIMIT) {
            return "LOW";
        }

        if (pixelCount <= MEDIUM_PIXEL_LIMIT) {
            return "MEDIUM";
        }

        return "HIGH";
    }
}
