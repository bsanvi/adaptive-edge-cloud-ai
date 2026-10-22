package com.edgecloud.edge.anomaly;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class AnomalyFeaturesTest {
    @Test void featureOrderAndScalingMatchCloud() {
        float[] f = AnomalyFeatures.extract(new double[]{1,1,1,1,1,1,1,1});
        assertEquals(8, f.length);
        assertEquals(1f, f[0]); // mean
        assertEquals(0f, f[1]); // population deviation
        assertEquals(0f, f[5]); // range
        assertEquals(0f, f[6]); // zero-crossing rate
        assertEquals(1f, f[7]); // positive fraction
    }
    @Test void rejectsShortOrInvalidSamples() {
        assertThrows(ResponseStatusException.class,
                () -> AnomalyFeatures.extract(new double[]{1,2,3}));
        assertThrows(ResponseStatusException.class,
                () -> AnomalyFeatures.extract(new double[]{1,2,3,4,5,6,7,Double.NaN}));
    }
}
