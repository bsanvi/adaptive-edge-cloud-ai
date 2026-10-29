
def normalize(
    value: float,
    minimum: float,
    maximum: float,
) -> float:
    """
    Convert a number to the range 0 to 1.
    """

    if maximum <= minimum:
        raise ValueError(
            "Maximum must be greater than minimum."
        )

    result = (value - minimum) / (maximum - minimum)

    return max(0.0, min(1.0, result))


from decision.config_loader import load_config


def normalize_metric(
    metric_name: str,
    value: float | None,
    config: dict | None = None,
) -> float | None:

    if value is None:
        return None

    if config is None:
        config = load_config()

    bounds = config["normalization"].get(metric_name)

    if bounds is None:
        raise ValueError(
            f"Unknown metric: {metric_name}"
        )

    return normalize(
        value,
        bounds["min"],
        bounds["max"],
    )


def desirability_score(
    metric_name: str,
    value: float | None,
    config: dict | None = None,
) -> float | None:

    normalized = normalize_metric(
        metric_name, value, config
    )

    if normalized is None:
        return None

    higher_is_better = {
        "battery_percent",
        "bandwidth_mbps",
    }

    lower_is_better = {
        "cpu_percent",
        "ram_percent",
        "latency_ms",
        "execution_time_ms",
        "energy_j",
        "cost",
        "carbon_g",
    }

    if metric_name in higher_is_better:
        return normalized

    if metric_name in lower_is_better:
        return 1.0 - normalized

    raise ValueError(
        f"No desirability direction for: {metric_name}"
    )
