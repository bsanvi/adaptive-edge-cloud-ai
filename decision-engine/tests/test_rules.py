
from decision.models import (
    TaskProfile,
    ExecutionEstimates,
    RouteEstimate,
)
from decision.rules import check_privacy, check_deadline
from decision.models import NetworkMetrics
from decision.rules import check_network
from decision.models import DecisionRequest
from decision.rules import apply_hard_rules
from decision.config_loader import load_config
import pytest


def make_task(privacy_required):
    return TaskProfile(
        task_id="task_001",
        task_type="computation",
        data_size_mb=5.0,
        complexity=0.5,
        deadline_ms=1000.0,
        privacy_required=privacy_required,
    )


def test_private_task():
    task = make_task(True)

    result = check_privacy(task)

    assert result["allowed_locations"] == ["edge"]
    assert "cloud" in result["rejected_locations"]
    assert "hybrid" in result["rejected_locations"]


def test_non_private_task():
    task = make_task(False)

    result = check_privacy(task)

    assert result["allowed_locations"] == [
        "edge",
        "cloud",
        "hybrid",
    ]
    assert result["rejected_locations"] == {}


def test_private_task_is_deterministic():
    task = make_task(True)

    first_result = check_privacy(task)
    second_result = check_privacy(task)

    assert first_result == second_result


def make_estimate(time_ms):
    return RouteEstimate(
        estimated_time_ms=time_ms,
        estimated_energy_j=10.0,
        estimated_cost=0.01,
        estimated_carbon_g=0.001,
    )


def test_deadline_feasibility():
    task = make_task(False)

    estimates = ExecutionEstimates(
        edge=make_estimate(700),
        cloud=make_estimate(1300),
        hybrid=make_estimate(900),
    )

    result = check_deadline(task, estimates)

    assert result["allowed_locations"] == [
        "edge",
        "hybrid",
    ]
    assert "cloud" in result["rejected_locations"]


def test_missing_cloud_estimate():
    task = make_task(False)

    estimates = ExecutionEstimates(
        edge=make_estimate(500),
        cloud=None,
        hybrid=None,
    )

    result = check_deadline(task, estimates)

    assert result["allowed_locations"] == ["edge"]
    assert "cloud" in result["rejected_locations"]
    assert "hybrid" in result["rejected_locations"]


def test_no_location_meets_deadline():
    task = make_task(False)

    estimates = ExecutionEstimates(
        edge=make_estimate(1500),
        cloud=make_estimate(2000),
        hybrid=make_estimate(1800),
    )

    result = check_deadline(task, estimates)

    assert result["allowed_locations"] == []
    assert len(result["rejected_locations"]) == 3


def test_exact_deadline_is_allowed():
    task = make_task(False)

    estimates = ExecutionEstimates(
        edge=make_estimate(1000),
    )

    result = check_deadline(task, estimates)

    assert result["allowed_locations"] == ["edge"]


def test_good_network():
    network = NetworkMetrics(
        latency_ms=30.0,
        bandwidth_mbps=50.0,
    )

    result = check_network(network)

    assert result["allowed_locations"] == [
        "edge", "cloud", "hybrid"
    ]


def test_high_latency():
    network = NetworkMetrics(
        latency_ms=300.0,
        bandwidth_mbps=50.0,
    )

    result = check_network(network)

    assert result["allowed_locations"] == ["edge"]
    assert "cloud" in result["rejected_locations"]


def test_low_bandwidth():
    network = NetworkMetrics(
        latency_ms=30.0,
        bandwidth_mbps=1.0,
    )

    result = check_network(network)

    assert result["allowed_locations"] == ["edge"]
    assert "hybrid" in result["rejected_locations"]


def test_both_network_conditions_bad():
    network = NetworkMetrics(
        latency_ms=300.0,
        bandwidth_mbps=1.0,
    )

    result = check_network(network)

    assert result["allowed_locations"] == ["edge"]

    reason = result["rejected_locations"]["cloud"]

    assert "Latency" in reason
    assert "Bandwidth" in reason


def test_combined_rules_reject_cloud_deadline():
    request = DecisionRequest.model_validate({
        "task": {
            "task_id": "task_101",
            "task_type": "computation",
            "data_size_mb": 5.0,
            "complexity": 0.5,
            "deadline_ms": 1000.0,
            "privacy_required": False,
        },
        "edge": {
            "cpu_percent": 40.0,
            "ram_percent": 50.0,
        },
        "network": {
            "latency_ms": 30.0,
            "bandwidth_mbps": 50.0,
        },
        "estimates": {
            "edge": {
                "estimated_time_ms": 700.0,
                "estimated_energy_j": 10.0,
                "estimated_cost": 0.0,
                "estimated_carbon_g": 0.001,
            },
            "cloud": {
                "estimated_time_ms": 1300.0,
                "estimated_energy_j": 12.0,
                "estimated_cost": 0.02,
                "estimated_carbon_g": 0.003,
            },
            "hybrid": {
                "estimated_time_ms": 900.0,
                "estimated_energy_j": 11.0,
                "estimated_cost": 0.01,
                "estimated_carbon_g": 0.002,
            },
        },
    })

    result = apply_hard_rules(request)

    assert result["allowed_locations"] == [
        "edge",
        "hybrid",
    ]
    assert "cloud" in result["rejected_locations"]


def test_combined_rules_private_task():
    request = DecisionRequest.model_validate({
        "task": {
            "task_id": "task_102",
            "task_type": "computation",
            "data_size_mb": 5.0,
            "complexity": 0.5,
            "deadline_ms": 1000.0,
            "privacy_required": True,
        },
        "edge": {
            "cpu_percent": 40.0,
            "ram_percent": 50.0,
        },
        "network": {
            "latency_ms": 30.0,
            "bandwidth_mbps": 50.0,
        },
        "estimates": {
            "edge": {
                "estimated_time_ms": 500.0,
                "estimated_energy_j": 10.0,
                "estimated_cost": 0.0,
                "estimated_carbon_g": 0.001,
            },
        },
    })

    result = apply_hard_rules(request)

    assert result["allowed_locations"] == ["edge"]
    assert "cloud" in result["rejected_locations"]
    assert "hybrid" in result["rejected_locations"]


def test_combined_rules_no_feasible_route():
    request = DecisionRequest.model_validate({
        "task": {
            "task_id": "task_103",
            "task_type": "computation",
            "data_size_mb": 5.0,
            "complexity": 0.5,
            "deadline_ms": 100.0,
            "privacy_required": True,
        },
        "edge": {
            "cpu_percent": 40.0,
            "ram_percent": 50.0,
        },
        "network": {
            "latency_ms": 300.0,
            "bandwidth_mbps": 1.0,
        },
        "estimates": {
            "edge": {
                "estimated_time_ms": 700.0,
                "estimated_energy_j": 10.0,
                "estimated_cost": 0.0,
                "estimated_carbon_g": 0.001,
            },
        },
    })

    result = apply_hard_rules(request)

    assert result["allowed_locations"] == []
    assert len(result["rejected_locations"]) == 3

    
def make_network_test_request():
    return DecisionRequest.model_validate({
        "task": {
            "task_id": "network_test",
            "task_type": "computation",
            "data_size_mb": 5.0,
            "complexity": 0.5,
            "deadline_ms": 2000.0,
            "privacy_required": False,
        },
        "edge": {
            "cpu_percent": 30.0,
            "ram_percent": 40.0,
        },
        "network": {
            "latency_ms": 150.0,
            "bandwidth_mbps": 50.0,
        },
        "estimates": {
            "edge": {
                "estimated_time_ms": 500.0,
                "estimated_energy_j": 10.0,
                "estimated_cost": 0.0,
                "estimated_carbon_g": 0.001,
            },
            "cloud": {
                "estimated_time_ms": 400.0,
                "estimated_energy_j": 15.0,
                "estimated_cost": 0.01,
                "estimated_carbon_g": 0.002,
            },
            "hybrid": {
                "estimated_time_ms": 600.0,
                "estimated_energy_j": 12.0,
                "estimated_cost": 0.005,
                "estimated_carbon_g": 0.0015,
            },
        },
    })

    
def test_network_threshold_from_config():
    request = make_network_test_request()

    config = load_config()
    config["network_rules"]["max_latency_ms"] = 100.0

    result = apply_hard_rules(request, config)

    assert "cloud" in result["rejected_locations"]
    assert "hybrid" in result["rejected_locations"]


def test_relaxed_network_threshold():
    request = make_network_test_request()

    config = load_config()
    config["network_rules"]["max_latency_ms"] = 300.0

    result = apply_hard_rules(request, config)

    assert result["allowed_locations"] == [
        "edge", "cloud", "hybrid"
    ]


def test_invalid_network_config():
    request = make_network_test_request()

    config = load_config()
    config["network_rules"]["max_latency_ms"] = -10.0

    with pytest.raises(ValueError):
        apply_hard_rules(request, config)


def test_privacy_rejection_code():
    request = make_network_test_request()
    request.task.privacy_required = True

    result = apply_hard_rules(request)

    cloud_codes = [
        item["code"]
        for item in result["rejection_details"]["cloud"]
    ]

    assert "PRIVACY_RESTRICTION" in cloud_codes


def test_deadline_rejection_code():
    request = make_network_test_request()
    request.task.deadline_ms = 100.0

    result = apply_hard_rules(request)

    edge_codes = [
        item["code"]
        for item in result["rejection_details"]["edge"]
    ]

    assert "DEADLINE_EXCEEDED" in edge_codes


def test_multiple_network_rejection_codes():
    request = make_network_test_request()

    request.network.latency_ms = 300.0
    request.network.bandwidth_mbps = 1.0

    result = apply_hard_rules(request)

    cloud_codes = [
        item["code"]
        for item in result["rejection_details"]["cloud"]
    ]

    assert "NETWORK_LATENCY_EXCEEDED" in cloud_codes
    assert "NETWORK_BANDWIDTH_LOW" in cloud_codes
