
import pytest

from decision.models import Location
from decision.switching import apply_switching


def make_scores(edge, cloud):
    return {
        "edge": {"adjusted_score": edge},
        "cloud": {"adjusted_score": cloud},
    }


def test_small_improvement_does_not_switch():
    scores = make_scores(0.80, 0.82)

    result = apply_switching(
        scores, Location.EDGE, 0.05
    )

    assert result["selected_location"] == "edge"
    assert result["switch_applied"] is False


def test_large_improvement_switches():
    scores = make_scores(0.70, 0.90)

    result = apply_switching(
        scores, Location.EDGE, 0.05
    )

    assert result["selected_location"] == "cloud"
    assert result["switch_applied"] is True


def test_first_decision_selects_best():
    scores = make_scores(0.70, 0.90)

    result = apply_switching(
        scores, None, 0.05
    )

    assert result["selected_location"] == "cloud"
    assert result["switch_applied"] is False


def test_rejected_previous_location():
    scores = {
        "edge": {"adjusted_score": 0.50}
    }

    result = apply_switching(
        scores, Location.CLOUD, 0.05
    )

    assert result["selected_location"] == "edge"
    assert result["switch_applied"] is True


def test_no_feasible_location():
    result = apply_switching(
        {}, Location.EDGE, 0.05
    )

    assert result["selected_location"] is None
    assert result["switch_applied"] is False


def test_invalid_threshold():
    scores = make_scores(0.80, 0.90)

    with pytest.raises(ValueError):
        apply_switching(
            scores, Location.EDGE, -0.1
        )


def test_noisy_scores_do_not_flip_flop():
    """
    Small score changes should not cause
    repeated switching.
    """

    scenarios = [
        (0.80, 0.81),
        (0.82, 0.81),
        (0.79, 0.82),
        (0.83, 0.80),
        (0.80, 0.83),
    ]

    previous = Location.EDGE
    decisions = []

    for edge_score, cloud_score in scenarios:
        scores = make_scores(edge_score, cloud_score)

        result = apply_switching(
            scores,
            previous,
            minimum_improvement=0.05,
        )

        decisions.append(result["selected_location"])

        previous = Location(
            result["selected_location"]
        )

    assert decisions == [
        "edge", "edge", "edge", "edge", "edge"
    ]


def test_large_change_triggers_switch():
    """
    The engine should still switch when
    the improvement is large enough.
    """

    scenarios = [
        (0.80, 0.82),
        (0.78, 0.81),
        (0.60, 0.90),
    ]

    previous = Location.EDGE
    decisions = []

    for edge_score, cloud_score in scenarios:
        result = apply_switching(
            make_scores(edge_score, cloud_score),
            previous,
            0.05,
        )

        decisions.append(result["selected_location"])

        previous = Location(
            result["selected_location"]
        )

    assert decisions == [
        "edge", "edge", "cloud"
    ]


def test_switching_sequence_is_deterministic():
    """
    Running the same sequence twice must
    produce identical decisions.
    """

    scenarios = [
        (0.80, 0.81),
        (0.75, 0.90),
        (0.88, 0.85),
        (0.95, 0.70),
    ]

    def run_sequence():
        previous = Location.EDGE
        decisions = []

        for edge_score, cloud_score in scenarios:
            result = apply_switching(
                make_scores(edge_score, cloud_score),
                previous,
                0.05,
            )

            selected = result["selected_location"]
            decisions.append(selected)
            previous = Location(selected)

        return decisions

    assert run_sequence() == run_sequence()

    
def test_reason_code_for_first_decision():
    result = apply_switching(
        make_scores(0.70, 0.90),
        None,
        0.05,
    )

    assert result["reason_code"] == "BEST_SCORE"


def test_reason_code_for_hysteresis():
    result = apply_switching(
        make_scores(0.80, 0.82),
        Location.EDGE,
        0.05,
    )

    assert result["reason_code"] == "SWITCH_AVOIDED"


def test_reason_code_for_infeasible_previous():
    scores = {
        "edge": {"adjusted_score": 0.75}
    }

    result = apply_switching(
        scores,
        Location.CLOUD,
        0.05,
    )

    assert result["reason_code"] == "PREVIOUS_INFEASIBLE"

