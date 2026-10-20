import cv2
import numpy as np

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


def test_execute_endpoint(tmp_path):
    input_path = tmp_path / "input.jpg"
    output_path = tmp_path / "output.jpg"

    image = np.zeros((800, 1200, 3), dtype=np.uint8)
    cv2.imwrite(str(input_path), image)

    response = client.post(
        "/edge/execute",
        json={
            "task_id": "api_test_001",
            "input_path": str(input_path),
            "output_path": str(output_path),
            "width": 640,
            "height": 480
        }
    )

    assert response.status_code == 200

    data = response.json()

    assert data["task_id"] == "api_test_001"
    assert data["execution_location"] == "edge"
    assert data["status"] == "success"
    assert data["task_type"] == "image_resize"

    assert data["result"]["input_width"] == 1200
    assert data["result"]["input_height"] == 800
    assert data["result"]["output_width"] == 640
    assert data["result"]["output_height"] == 480

    assert data["result"]["input_size_bytes"] > 0
    assert data["result"]["output_size_bytes"] > 0
    assert data["execution_time_ms"] >= 0

    assert data["error"] is None