
from decision.models import DecisionRequest
from decision.engine import make_decision
from decision.models import Location
from decision.config_loader import load_config

def make_request(
    privacy=False,
    deadline=2000.0,
    edge_time=1000.0,
    cloud_time=500.0,
    hybrid_time=800.0,
):
    return DecisionRequest.model_validate({
        "task": {
            "task_id": "task_001",
            "task_type": "computation",
            "data_size_mb": 5.0,
            "complexity": 0.5,
            "deadline_ms": deadline,
            "privacy_required": privacy,
        },
        "edge": {
            "cpu_percent": 30.0,
            "ram_percent": 40.0,
            "battery_percent": 80.0,
            "power_plugged": False,
        },
        "network": {
            "latency_ms": 30.0,
            "bandwidth_mbps": 50.0,
        },
        "estimates": {
            "edge": {
                "estimated_time_ms": edge_time,
                "estimated_energy_j": 20.0,
                "estimated_cost": 0.0,
                "estimated_carbon_g": 0.01,
            },
            "cloud": {
                "estimated_time_ms": cloud_time,
                "estimated_energy_j": 20.0,
                "estimated_cost": 0.0,
                "estimated_carbon_g": 0.01,
            },
            "hybrid": {
                "estimated_time_ms": hybrid_time,
                "estimated_energy_j": 20.0,
                "estimated_cost": 0.0,
                "estimated_carbon_g": 0.01,
            },
        },
    })


def test_private_task_selects_edge():
    request = make_request(privacy=True)

    result = make_decision(request)

    assert result["status"] == "selected"
    assert result["selected_location"] == "edge"
    assert "cloud" in result["rejected_locations"]


def test_fast_cloud_selected():
    request = make_request(
        edge_time=1500.0,
        cloud_time=100.0,
        hybrid_time=1200.0,
    )

    result = make_decision(request)

    assert result["selected_location"] == "cloud"


def test_no_feasible_route():
    request = make_request(
        privacy=True,
        deadline=100.0,
        edge_time=1000.0,
        cloud_time=500.0,
        hybrid_time=800.0,
    )

    result = make_decision(request)

    assert result["status"] == "no_feasible_route"
    assert result["selected_location"] is None
    assert result["scores"] == {}


def test_engine_keeps_previous_route_with_large_threshold():
    request = make_request(
        edge_time=1500.0,
        cloud_time=100.0,
        hybrid_time=1200.0,
    )

    request.previous_location = Location.EDGE

    config = load_config()
    config["switching"]["minimum_improvement"] = 1.0

    result = make_decision(request, config)

    assert result["selected_location"] == "edge"
    assert result["switch_applied"] is False


def test_engine_rejects_infeasible_previous_route():
    request = make_request(privacy=True)

    request.previous_location = Location.CLOUD

    result = make_decision(request)

    assert result["selected_location"] == "edge"
    assert result["switch_applied"] is True
    assert "cloud" in result["rejected_locations"]
