
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
        return score(metrics, ScoringConfiguration.defaults(), "BALANCED");
    }

    public static ScoreResult score(Metrics metrics, ScoringConfiguration config, String profile) {
        var weights = config.weights(profile);
        var bounds = config.bounds();
        if (metrics == null) {
            throw new IllegalArgumentException(
                "Metrics cannot be null"
            );
        }

        for (double value : new double[]{metrics.timeMs(), metrics.energyJ(), metrics.monetaryCost(), metrics.carbonG()}) {
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Scoring metrics must be finite and nonnegative");
        }
        double time = Normalizer.lowerIsBetter(
            metrics.timeMs(), bounds.get("time").min(), bounds.get("time").max()
        );

        double energy = Normalizer.lowerIsBetter(
            metrics.energyJ(), bounds.get("energy").min(), bounds.get("energy").max()
        );

        double cost = Normalizer.lowerIsBetter(
            metrics.monetaryCost(), bounds.get("cost").min(), bounds.get("cost").max()
        );

        double carbon = Normalizer.lowerIsBetter(
            metrics.carbonG(), bounds.get("carbon").min(), bounds.get("carbon").max()
        );

        double timeContribution = time * weights.get("time");
        double energyContribution = energy * weights.get("energy");
        double costContribution = cost * weights.get("cost");
        double carbonContribution = carbon * weights.get("carbon");

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
