from fastapi.testclient import TestClient

from edge.api import app


client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")

    assert response.status_code == 200

    data = response.json()

    assert data["status"] == "ok"
    assert data["component"] == "edge"


def test_metrics_endpoint():
    response = client.get("/edge/metrics")

    assert response.status_code == 200

    data = response.json()

    assert "cpu_percent" in data
    assert "ram_percent" in data
    assert "battery_percent" in data
    assert "timestamp" in data


def test_execute_endpoint():
    response = client.post(
        "/edge/execute",
        json={
            "task_id": "api_test_001",
            "number_count": 4
        }
    )

    assert response.status_code == 200

    data = response.json()

    assert data["task_id"] == "api_test_001"
    assert data["execution_location"] == "edge"
    assert data["status"] == "success"
    assert data["task_type"] == "sum_squares"
    assert data["result"] == 14
    assert data["execution_time_ms"] >= 0
    assert data["error"] is None