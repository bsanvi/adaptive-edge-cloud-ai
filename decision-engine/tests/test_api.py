
from fastapi.testclient import TestClient

from decision.api import app


client = TestClient(app)


def make_payload():
    """Mock input from other team members."""
    return {
        "task": {
            "task_id": "api_test_001",
            "task_type": "computation",
            "data_size_mb": 5.0,
            "complexity": 0.5,
            "deadline_ms": 2000.0,
            "privacy_required": True,
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
                "estimated_time_ms": 700.0,
                "estimated_energy_j": 10.0,
                "estimated_cost": 0.0,
                "estimated_carbon_g": 0.001,
            },
            "cloud": {
                "estimated_time_ms": 300.0,
                "estimated_energy_j": 20.0,
                "estimated_cost": 0.02,
                "estimated_carbon_g": 0.003,
            },
            "hybrid": {
                "estimated_time_ms": 500.0,
                "estimated_energy_j": 15.0,
                "estimated_cost": 0.01,
                "estimated_carbon_g": 0.002,
            },
        },
        "previous_location": None,
    }


def test_health_endpoint():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_private_task_api():
    response = client.post(
        "/decision/evaluate",
        json=make_payload(),
    )

    assert response.status_code == 200

    data = response.json()

    assert data["selected_location"] == "edge"
    assert data["status"] == "selected"

    assert (
        data["rejection_details"]["cloud"][0]["code"]
        == "PRIVACY_RESTRICTION"
    )


def test_invalid_cpu_api():
    payload = make_payload()
    payload["edge"]["cpu_percent"] = 150.0

    response = client.post(
        "/decision/evaluate",
        json=payload,
    )

    assert response.status_code == 422


def test_no_feasible_route_api():
    payload = make_payload()

    payload["task"]["deadline_ms"] = 100.0

    response = client.post(
        "/decision/evaluate",
        json=payload,
    )

    assert response.status_code == 200

    data = response.json()

    assert data["status"] == "no_feasible_route"
    assert data["selected_location"] is None
    assert data["scores"] == {}


def test_missing_cloud_estimate_api():
    payload = make_payload()

    payload["task"]["privacy_required"] = False

    payload["estimates"]["cloud"] = None
    payload["estimates"]["hybrid"] = None

    response = client.post(
        "/decision/evaluate",
        json=payload,
    )

    assert response.status_code == 200

    data = response.json()

    assert data["selected_location"] == "edge"
    assert data["status"] == "selected"

    assert (
        data["rejection_details"]["cloud"][0]["code"]
        == "MISSING_ESTIMATE"
    )
