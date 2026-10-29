
from experiments.compare_baselines import (
    compare_baselines,
)


def test_comparison_generates_all_results():
    results = compare_baselines()

    # 3 scenarios x 4 strategies = 12 results
    assert len(results) == 12


def test_comparison_is_reproducible():
    first = compare_baselines(seed=42)
    second = compare_baselines(seed=42)

    assert first == second


def test_always_cloud_violates_privacy():
    results = compare_baselines()

    matching = [
        item for item in results
        if item["scenario"] == "private_task"
        and item["strategy"] == "always_cloud"
    ]

    assert len(matching) == 1

    result = matching[0]

    assert result["selected_location"] == "cloud"
    assert result["feasible"] is False
    assert "PRIVACY_RESTRICTION" in (
        result["violation_codes"]
    )


def test_engine_never_selects_infeasible_route():
    results = compare_baselines()

    engine_results = [
        item for item in results
        if item["strategy"] == "decision_engine"
    ]

    for item in engine_results:
        assert item["feasible"] or item["no_route"]


def test_impossible_deadline_has_no_engine_route():
    results = compare_baselines()

    matching = [
        item for item in results
        if item["scenario"] == "impossible_deadline"
        and item["strategy"] == "decision_engine"
    ]

    assert len(matching) == 1
    assert matching[0]["selected_location"] is None
    assert matching[0]["no_route"] is True
