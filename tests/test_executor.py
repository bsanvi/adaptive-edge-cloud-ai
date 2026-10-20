import cv2
import numpy as np
import pytest

from edge.executor import execute_image_resize, execute_sum_squares


def test_execute_sum_squares():
    result = execute_sum_squares(4)

    assert result["task_type"] == "sum_squares"
    assert result["number_count"] == 4
    assert result["result"] == 14
    assert result["execution_time_ms"] >= 0
    assert result["status"] == "success"


def test_invalid_number_count():
    with pytest.raises(ValueError):
        execute_sum_squares(0)


def test_execute_image_resize(tmp_path):
    # Create temporary file paths.
    input_path = tmp_path / "input.jpg"
    output_path = tmp_path / "output.jpg"

    # Create a simple 1200 x 800 black image.
    image = np.zeros((800, 1200, 3), dtype=np.uint8)

    # Save the temporary input image.
    cv2.imwrite(str(input_path), image)

    result = execute_image_resize(
        str(input_path),
        str(output_path),
        640,
        480
    )

    assert result["task_type"] == "image_resize"
    assert result["status"] == "success"

    assert result["input_width"] == 1200
    assert result["input_height"] == 800

    assert result["output_width"] == 640
    assert result["output_height"] == 480

    assert result["input_size_bytes"] > 0
    assert result["output_size_bytes"] > 0

    assert isinstance(result["size_reduction_percent"], float)

    assert result["execution_time_ms"] >= 0

    assert output_path.exists()