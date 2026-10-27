
import json
import random
from pathlib import Path

from decision.engine import make_decision
from decision.models import DecisionRequest
from decision.rules import apply_hard_rules

from experiments.baselines import (
    always_edge,
    always_cloud,
    random_location,
)


SCENARIO_FILE = (
    Path(__file__).resolve().parent / "scenarios.json"
)


def compare_baselines(seed=42):
    # Use one seeded generator for reproducibility.
    rng = random.Random(seed)

    with open(
        SCENARIO_FILE, "r", encoding="utf-8"
    ) as file:
        scenarios = json.load(file)

    results = []

    for scenario in scenarios:
        request = DecisionRequest.model_validate(
            scenario["request"]
        )

        # Calculate which routes are actually feasible.
        rules = apply_hard_rules(request)
        feasible = rules["allowed_locations"]

        # Get choices from the four strategies.
        choices = {
            "always_edge": always_edge(),
            "always_cloud": always_cloud(),
            "random": random_location(rng),
            "decision_engine": make_decision(
                request
            )["selected_location"],
        }

        for strategy, location in choices.items():
            is_feasible = location in feasible

            # None means the engine declined to select
            # a route because no route was feasible.
            no_route = location is None

            violation_codes = []

            if location is not None:
                violation_codes = [
                    detail["code"]
                    for detail in rules[
                        "rejection_details"
                    ].get(location, [])
                ]

            results.append({
                "scenario": scenario["name"],
                "strategy": strategy,
                "selected_location": location,
                "feasible": is_feasible,
                "no_route": no_route,
                "violation_codes": violation_codes,
            })

    return results


if __name__ == "__main__":
    results = compare_baselines()

    for item in results:
        print(
            f"{item['scenario']:22} "
            f"{item['strategy']:17} "
            f"Selected={str(item['selected_location']):7} "
            f"Feasible={item['feasible']}"
        )
