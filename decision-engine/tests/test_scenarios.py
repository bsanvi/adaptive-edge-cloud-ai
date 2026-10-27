
from pathlib import Path

from decision.engine import make_decision
from decision.models import DecisionRequest
from experiments.run_scenarios import run_scenarios
from copy import deepcopy

from decision.models import EdgeMetrics, Location
from decision.scorer import score_location
from decision.config_loader import load_config


def test_all_scenarios_pass():
    results = run_scenarios()

    assert len(results) == 3
    assert all(item["passed"] for item in results)


def test_scenario_results_are_deterministic():
    first = run_scenarios()
    second = run_scenarios()

    assert first == second


def test_scenario_file_exists():
    scenario_file = (
        Path(__file__).resolve().parent.parent
        / "experiments"
        / "scenarios.json"
    )

    assert scenario_file.is_file()


def test_low_battery_reduces_edge_score():
    config = load_config()

    from decision.models import RouteEstimate

    estimate = RouteEstimate(
        estimated_time_ms=1000.0,
        estimated_energy_j=20.0,
        estimated_cost=0.0,
        estimated_carbon_g=0.01,
    )

    high_battery = EdgeMetrics(
        cpu_percent=30.0,
        ram_percent=40.0,
        battery_percent=90.0,
        power_plugged=False,
    )

    low_battery = EdgeMetrics(
        cpu_percent=30.0,
        ram_percent=40.0,
        battery_percent=10.0,
        power_plugged=False,
    )

    high_result = score_location(
        Location.EDGE,
        estimate,
        high_battery,
        config,
    )

    low_result = score_location(
        Location.EDGE,
        estimate,
        low_battery,
        config,
    )

    assert (
        low_result["device_readiness"]
        < high_result["device_readiness"]
    )

    assert (
        low_result["adjusted_score"]
        < high_result["adjusted_score"]
    )


def test_missing_cloud_estimate_is_rejected():
    from test_engine import make_request

    request = make_request(privacy=False)

    request.estimates.cloud = None

    result = make_decision(request)

    cloud_codes = [
        item["code"]
        for item in result["rejection_details"]["cloud"]
    ]

    assert "MISSING_ESTIMATE" in cloud_codes

    assert result["selected_location"] != "cloud"
