
package com.adaptive.decision.scoring;

import java.util.Map;

public final class WeightedScorer {

    private WeightedScorer() {
    }

    public record Metrics(
        double timeMs,
        double energyJ,
        double monetaryCost,
        double carbonG
    ) {}

    public record ScoreResult(
        double score,
        Map<String, Double> components,
        Map<String, Double> contributions
    ) {}

    // Normalization bounds from Python config.json
    private static final double MAX_TIME_MS = 5000.0;
    private static final double MAX_ENERGY_J = 100.0;
    private static final double MAX_COST = 1.0;
    private static final double MAX_CARBON_G = 0.1;

    // Weights from Python config.json
    private static final double TIME_WEIGHT = 0.50;
    private static final double ENERGY_WEIGHT = 0.20;
    private static final double COST_WEIGHT = 0.20;
    private static final double CARBON_WEIGHT = 0.10;

    public static ScoreResult score(Metrics metrics) {

        if (metrics == null) {
            throw new IllegalArgumentException(
                "Metrics cannot be null"
            );
        }

        double time = Normalizer.lowerIsBetter(
            metrics.timeMs(), 0.0, MAX_TIME_MS
        );

        double energy = Normalizer.lowerIsBetter(
            metrics.energyJ(), 0.0, MAX_ENERGY_J
        );

        double cost = Normalizer.lowerIsBetter(
            metrics.monetaryCost(), 0.0, MAX_COST
        );

        double carbon = Normalizer.lowerIsBetter(
            metrics.carbonG(), 0.0, MAX_CARBON_G
        );

        double timeContribution = time * TIME_WEIGHT;
        double energyContribution = energy * ENERGY_WEIGHT;
        double costContribution = cost * COST_WEIGHT;
        double carbonContribution = carbon * CARBON_WEIGHT;

        double total =
            timeContribution +
            energyContribution +
            costContribution +
            carbonContribution;

        return new ScoreResult(
            total,
            Map.of(
                "time", time,
                "energy", energy,
                "cost", cost,
                "carbon", carbon
            ),
            Map.of(
                "time", timeContribution,
                "energy", energyContribution,
                "cost", costContribution,
                "carbon", carbonContribution
            )
        );
    }
}
