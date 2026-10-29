
from decision.explainer import explain_decision


def test_explanation_for_selected_route():
    result = {
        "status": "selected",
        "selected_location": "cloud",
        "scores": {
            "cloud": {
                "adjusted_score": 0.91,
            }
        },
        "rejected_locations": {},
        "switch_applied": False,
        "switch_reason": "No previous location available.",
    }

    explanation = explain_decision(result)

    assert "Cloud selected" in explanation
    assert "0.9100" in explanation
    assert "highest-scoring" in explanation


def test_explanation_for_privacy_rejection():
    result = {
        "status": "selected",
        "selected_location": "edge",
        "scores": {
            "edge": {
                "adjusted_score": 0.75,
            }
        },
        "rejected_locations": {
            "cloud": [
                "Task requires local processing."
            ],
        },
        "switch_applied": False,
        "switch_reason": "No previous location available.",
    }

    explanation = explain_decision(result)

    assert "Edge selected" in explanation
    assert "Cloud rejected" in explanation
    assert "local processing" in explanation


def test_explanation_for_hysteresis():
    result = {
        "status": "selected",
        "selected_location": "edge",
        "scores": {
            "edge": {
                "adjusted_score": 0.80,
            },
            "cloud": {
                "adjusted_score": 0.82,
            },
        },
        "rejected_locations": {},
        "switch_applied": False,
        "switch_reason": (
            "Improvement is too small to justify switching."
        ),
    }

    explanation = explain_decision(result)

    assert "previous location was retained" in explanation


def test_explanation_for_no_feasible_route():
    result = {
        "status": "no_feasible_route",
        "selected_location": None,
        "scores": {},
        "rejected_locations": {
            "edge": ["Deadline exceeded."],
            "cloud": ["Privacy restriction."],
            "hybrid": ["Privacy restriction."],
        },
    }

    explanation = explain_decision(result)

    assert "No feasible execution location" in explanation
    assert "Edge rejected" in explanation
    assert "Cloud rejected" in explanation
