
from decision.config_loader import load_config
from decision.models import RouteEstimate
from decision.normalizer import desirability_score
from decision.models import EdgeMetrics, Location


def score_route(
    estimate: RouteEstimate,
    config: dict | None = None,
) -> dict:

    if config is None:
        config = load_config()

    weights = config["weights"]

    metrics = {
        "execution_time": (
            "execution_time_ms",
            estimate.estimated_time_ms,
        ),
        "energy": (
            "energy_j",
            estimate.estimated_energy_j,
        ),
        "cost": (
            "cost",
            estimate.estimated_cost,
        ),
        "carbon": (
            "carbon_g",
            estimate.estimated_carbon_g,
        ),
    }

    expected_weights = set(metrics)

    if set(weights) != expected_weights:
        raise ValueError(
            "Weights must include exactly: "
            "execution_time, energy, cost, carbon."
        )

    if any(
        not isinstance(weight, (int, float))
        or isinstance(weight, bool)
        or not 0 <= weight <= 1
        for weight in weights.values()
    ):
        raise ValueError(
            "Each weight must be between 0 and 1."
        )

    if abs(sum(weights.values()) - 1.0) > 1e-9:
        raise ValueError(
            "Weights must sum to 1."
        )

    components = {}
    weighted_contributions = {}

    for name, (metric_name, value) in metrics.items():
        desirability = desirability_score(
            metric_name,
            value,
            config,
        )

        components[name] = desirability

        weighted_contributions[name] = (
            weights[name] * desirability
        )

    final_score = sum(weighted_contributions.values())

    return {
        "score": round(final_score, 4),
        "components": components,
        "weighted_contributions": weighted_contributions,
    }


def calculate_device_readiness(
    edge: EdgeMetrics,
    config: dict,
) -> float:
    """
    Calculate how ready the edge device is
    to perform local processing.
    """

    cpu = desirability_score(
        "cpu_percent", edge.cpu_percent, config
    )

    ram = desirability_score(
        "ram_percent", edge.ram_percent, config
    )

    values = [cpu, ram]

    # Battery matters when the device is unplugged.
    # Missing battery data is ignored.
    if edge.power_plugged is False:
        battery = desirability_score(
            "battery_percent",
            edge.battery_percent,
            config,
        )

        if battery is not None:
            values.append(battery)

    return sum(values) / len(values)


def score_location(
    location: Location,
    estimate: RouteEstimate,
    edge: EdgeMetrics,
    config: dict | None = None,
) -> dict:

    if config is None:
        config = load_config()

    base_result = score_route(estimate, config)
    base_score = base_result["score"]

    strength = config["device_adjustment"]["strength"]

    if (
        isinstance(strength, bool)
        or not isinstance(strength, (int, float))
        or not 0 <= strength <= 1
    ):
        raise ValueError(
            "Device adjustment strength must be 0 to 1."
        )

    # Remote-only execution does not use local
    # device readiness in this scoring model.
    if location == Location.CLOUD:
        return {
            **base_result,
            "base_score": base_score,
            "device_readiness": None,
            "adjusted_score": base_score,
        }

    if location not in (Location.EDGE, Location.HYBRID):
        raise ValueError("Invalid execution location.")

    readiness = calculate_device_readiness(edge, config)

    adjusted_score = (
        (1 - strength) * base_score
        + strength * readiness
    )

    return {
        **base_result,
        "base_score": base_score,
        "device_readiness": round(readiness, 4),
        "adjusted_score": round(adjusted_score, 4),
    }
