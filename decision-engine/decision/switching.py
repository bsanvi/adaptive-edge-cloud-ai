
import math

from decision.models import Location


def apply_switching(
    scores: dict,
    previous_location: Location | None,
    minimum_improvement: float,
) -> dict:

    if (
        isinstance(minimum_improvement, bool)
        or not isinstance(minimum_improvement, (int, float))
        or not math.isfinite(minimum_improvement)
        or not 0 <= minimum_improvement <= 1
    ):
        raise ValueError(
            "Minimum improvement must be between 0 and 1."
        )

    if not scores:
        return {
            "selected_location": None,
            "switch_applied": False,
            "reason_code": "NO_FEASIBLE_ROUTE",
            "reason": "No feasible execution location.",
        }

    best_location = max(
        scores,
        key=lambda name: scores[name]["adjusted_score"],
    )

    if previous_location is None:
        return {
            "selected_location": best_location,
            "switch_applied": False,
            "reason_code": "BEST_SCORE",
            "reason": "No previous location available.",
        }

    previous = previous_location.value

    if previous not in scores:
        return {
            "selected_location": best_location,
            "switch_applied": best_location != previous,
            "reason_code": "PREVIOUS_INFEASIBLE",
            "reason": "Previous location is no longer feasible.",
        }

    previous_score = scores[previous]["adjusted_score"]
    best_score = scores[best_location]["adjusted_score"]

    improvement = best_score - previous_score

    if improvement <= minimum_improvement:
        return {
            "selected_location": previous,
            "switch_applied": False,
            "reason_code": "SWITCH_AVOIDED",
            "reason": (
                "Improvement is too small to justify switching."
            ),
        }

    return {
        "selected_location": best_location,
        "switch_applied": True,
        "reason_code": "SCORE_IMPROVEMENT",
        "reason": (
            f"Score improvement {improvement:.4f} "
            f"exceeds threshold {minimum_improvement:.4f}."
        ),
    }
