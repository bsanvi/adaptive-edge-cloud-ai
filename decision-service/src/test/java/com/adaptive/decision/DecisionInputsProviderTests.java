
package com.adaptive.decision;

import com.adaptive.decision.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecisionInputsProviderTests {

    @Test
    void rejectsUnconfiguredLiveInputs() {

        DecisionInputsProvider provider =
            new UnconfiguredDecisionInputsProvider();

        IllegalStateException error =
            assertThrows(
                IllegalStateException.class,
                () -> provider.resolve(null)
            );

        assertTrue(
            error.getMessage().contains(
                "not configured"
            )
        );
    }
}
