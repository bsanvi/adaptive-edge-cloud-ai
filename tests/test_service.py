import cv2
import numpy as np

from edge.service import run_edge_task


def test_run_edge_task_success(tmp_path):
    input_path = tmp_path / "input.jpg"
    output_path = tmp_path / "output.jpg"

    # Create a temporary 1200 x 800 image.
    image = np.zeros((800, 1200, 3), dtype=np.uint8)
    cv2.imwrite(str(input_path), image)

    result = run_edge_task(
        task_id="test_001",
        input_path=str(input_path),
        output_path=str(output_path),
        width=640,
        height=480
    )

    assert result["task_id"] == "test_001"
    assert result["execution_location"] == "edge"
    assert result["status"] == "success"
    assert result["task_type"] == "image_resize"

    assert result["task_input"]["width"] == 640
    assert result["task_input"]["height"] == 480

    assert result["result"]["input_width"] == 1200
    assert result["result"]["input_height"] == 800

    assert result["result"]["output_width"] == 640
    assert result["result"]["output_height"] == 480

    assert result["execution_time_ms"] >= 0

    assert "cpu_percent" in result["metrics_before"]
    assert "ram_percent" in result["metrics_before"]

    assert "cpu_percent" in result["metrics_after"]
    assert "ram_percent" in result["metrics_after"]

    assert result["error"] is None


def test_run_edge_task_failure(tmp_path):
    missing_input = tmp_path / "missing.jpg"
    output_path = tmp_path / "output.jpg"

    result = run_edge_task(
        task_id="test_002",
        input_path=str(missing_input),
        output_path=str(output_path),
        width=640,
        height=480
    )

    assert result["task_id"] == "test_002"
    assert result["execution_location"] == "edge"
    assert result["status"] == "failed"
    assert result["task_type"] == "image_resize"

    assert result["result"] is None
    assert result["execution_time_ms"] is None
    assert result["error"] is not None