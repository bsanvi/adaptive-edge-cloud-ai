
import pytest

from decision.models import RouteEstimate
from decision.scorer import score_route
from decision.models import EdgeMetrics, Location
from decision.scorer import score_location


def make_estimate(
    time_ms,
    energy_j,
    cost=0.0,
    carbon=0.0,
):
    return RouteEstimate(
        estimated_time_ms=time_ms,
        estimated_energy_j=energy_j,
        estimated_cost=cost,
        estimated_carbon_g=carbon,
    )


def test_route_score():
    estimate = make_estimate(
        time_ms=1000,
        energy_j=20,
        cost=0.2,
        carbon=0.02,
    )

    result = score_route(estimate)

    # Each metric has desirability 0.8.
    # Total weights sum to 1.
    assert result["score"] == pytest.approx(0.8)


def test_faster_route_scores_higher():
    fast = make_estimate(500, 20)
    slow = make_estimate(2000, 20)

    assert (
        score_route(fast)["score"]
        > score_route(slow)["score"]
    )


def test_lower_cost_scores_higher():
    cheap = make_estimate(1000, 20, cost=0.1)
    expensive = make_estimate(1000, 20, cost=0.8)

    assert (
        score_route(cheap)["score"]
        > score_route(expensive)["score"]
    )


def test_lower_carbon_scores_higher():
    low_carbon = make_estimate(
        1000, 20, carbon=0.01
    )
    high_carbon = make_estimate(
        1000, 20, carbon=0.08
    )

    assert (
        score_route(low_carbon)["score"]
        > score_route(high_carbon)["score"]
    )


def test_contributions_sum_to_score():
    estimate = make_estimate(
        1000, 20, cost=0.2, carbon=0.02
    )

    result = score_route(estimate)

    total = sum(
        result["weighted_contributions"].values()
    )

    assert total == pytest.approx(result["score"])


def test_invalid_weights_rejected():
    estimate = make_estimate(1000, 20)

    config = {
        "normalization": {
            "execution_time_ms": {
                "min": 0, "max": 5000
            },
            "energy_j": {
                "min": 0, "max": 100
            },
            "cost": {
                "min": 0, "max": 1
            },
            "carbon_g": {
                "min": 0, "max": 0.1
            },
        },
        "weights": {
            "execution_time": 0.9,
            "energy": 0.5,
            "cost": 0.2,
            "carbon": 0.1,
        },
    }

    with pytest.raises(ValueError):
        score_route(estimate, config)


def test_busy_edge_reduces_score():
    estimate = make_estimate(1000, 20)

    edge = EdgeMetrics(
        cpu_percent=95.0,
        ram_percent=90.0,
        battery_percent=10.0,
        power_plugged=False,
    )

    result = score_location(
        Location.EDGE, estimate, edge
    )

    assert result["adjusted_score"] < result["base_score"]


def test_cloud_score_unchanged():
    estimate = make_estimate(1000, 20)

    edge = EdgeMetrics(
        cpu_percent=95.0,
        ram_percent=90.0,
    )

    result = score_location(
        Location.CLOUD, estimate, edge
    )

    assert result["adjusted_score"] == result["base_score"]
    assert result["device_readiness"] is None


def test_missing_battery_handled():
    estimate = make_estimate(1000, 20)

    edge = EdgeMetrics(
        cpu_percent=40.0,
        ram_percent=50.0,
    )

    result = score_location(
        Location.EDGE, estimate, edge
    )

    assert 0 <= result["adjusted_score"] <= 1


def test_device_readiness_is_deterministic():
    estimate = make_estimate(1000, 20)

    edge = EdgeMetrics(
        cpu_percent=40.0,
        ram_percent=50.0,
        battery_percent=80.0,
        power_plugged=False,
    )

    first = score_location(Location.HYBRID, estimate, edge)
    second = score_location(Location.HYBRID, estimate, edge)

    assert first == second
