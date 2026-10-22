package com.adaptive.cloud;

import org.springframework.stereotype.Component;

/** features-v1: population deviation, identity scaling, crossings/(n-1), zeros do not cross. */
@Component
public class FeatureExtractor {
    public float[] extract(double[] samples) {
        if (samples == null || samples.length < 8 || samples.length > 4096)
            throw new ApiFailure(400, "VALIDATION_ERROR", "samples must contain 8 to 4096 values");
        double sum = 0, squares = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        int positive = 0, crossings = 0;
        for (int i = 0; i < samples.length; i++) {
            double x = samples[i];
            if (!Double.isFinite(x) || Math.abs(x) > 1_000_000)
                throw new ApiFailure(400, "OUT_OF_RANGE", "Samples must be finite and within +/-1000000");
            sum += x; squares += x * x; min = Math.min(min, x); max = Math.max(max, x);
            if (x > 0) positive++;
            if (i > 0 && ((x > 0 && samples[i-1] < 0) || (x < 0 && samples[i-1] > 0))) crossings++;
        }
        double mean = sum / samples.length, variance = 0;
        for (double x : samples) variance += (x - mean) * (x - mean);
        return new float[]{(float)mean, (float)Math.sqrt(variance / samples.length), (float)min,
            (float)max, (float)Math.sqrt(squares / samples.length), (float)(max-min),
            (float)crossings/(samples.length-1), (float)positive/samples.length};
    }
}
