
package com.adaptive.decision;

import com.adaptive.decision.scoring.Normalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalizerTests {

    @Test
    void minimumNormalizesToZero() {
        assertEquals(
            0.0,
            Normalizer.normalize(0, 0, 100),
            0.000001
        );
    }

    @Test
    void maximumNormalizesToOne() {
        assertEquals(
            1.0,
            Normalizer.normalize(100, 0, 100),
            0.000001
        );
    }

    @Test
    void midpointNormalizesToHalf() {
        assertEquals(
            0.5,
            Normalizer.normalize(50, 0, 100),
            0.000001
        );
    }

    @Test
    void valuesOutsideRangeAreClamped() {
        assertEquals(
            0.0,
            Normalizer.normalize(-50, 0, 100)
        );

        assertEquals(
            1.0,
            Normalizer.normalize(150, 0, 100)
        );
    }

    @Test
    void lowerExecutionTimeIsBetter() {
        double fast = Normalizer.lowerIsBetter(
            400, 0, 2000
        );

        double slow = Normalizer.lowerIsBetter(
            1500, 0, 2000
        );

        assertTrue(fast > slow);
    }

    @Test
    void higherBatteryIsBetter() {
        double high = Normalizer.higherIsBetter(
            90, 0, 100
        );

        double low = Normalizer.higherIsBetter(
            10, 0, 100
        );

        assertTrue(high > low);
    }

    @Test
    void rejectsInvalidRange() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Normalizer.normalize(
                50, 100, 100
            )
        );
    }

    @Test
    void rejectsNonFiniteInputs() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Normalizer.normalize(
                Double.NaN, 0, 100
            )
        );
    }
}
