
import json
from pathlib import Path


CONFIG_PATH = (
    Path(__file__).resolve().parent.parent
    / "config"
    / "config.json"
)


def load_config(path=CONFIG_PATH):
    with open(path, "r", encoding="utf-8") as file:
        config = json.load(file)

    if "normalization" not in config:
        raise ValueError(
            "Missing normalization configuration."
        )

    for metric, bounds in config["normalization"].items():
        if not isinstance(bounds, dict):
            raise ValueError(
                f"Invalid bounds for {metric}."
            )

        minimum = bounds.get("min")
        maximum = bounds.get("max")

        if (
            type(minimum) not in (int, float)
            or type(maximum) not in (int, float)
        ):
            raise ValueError(
                f"Invalid numeric bounds for {metric}."
            )

        if not (
            float("-inf") < minimum < maximum < float("inf")
        ):
            raise ValueError(
                f"Invalid range for {metric}."
            )

    return config
