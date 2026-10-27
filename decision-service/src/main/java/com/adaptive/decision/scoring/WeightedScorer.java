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

    public static ScoreResult score(Metrics metrics) {

        if (metrics == null) {
            throw new IllegalArgumentException(
                "Metrics cannot be null"
            );
        }

        double time = Normalizer.lowerIsBetter(
            metrics.timeMs(), 0.0, 10000.0
        );

        double energy = Normalizer.lowerIsBetter(
            metrics.energyJ(), 0.0, 1000.0
        );

        double cost = Normalizer.lowerIsBetter(
            metrics.monetaryCost(), 0.0, 1.0
        );

        double carbon = Normalizer.lowerIsBetter(
            metrics.carbonG(), 0.0, 100.0
        );

        double timeContribution = time * 0.50;
        double energyContribution = energy * 0.20;
        double costContribution = cost * 0.20;
        double carbonContribution = carbon * 0.10;

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
