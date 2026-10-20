import cv2
import numpy as np
import pytest

from edge.executor import execute_image_resize


def test_execute_image_resize(tmp_path):
    input_path = tmp_path / "input.jpg"
    output_path = tmp_path / "output.jpg"

    # Create a temporary 1200 x 800 image.
    image = np.zeros((800, 1200, 3), dtype=np.uint8)
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


def test_invalid_image_dimensions(tmp_path):
    input_path = tmp_path / "input.jpg"
    output_path = tmp_path / "output.jpg"

    image = np.zeros((800, 1200, 3), dtype=np.uint8)
    cv2.imwrite(str(input_path), image)

    with pytest.raises(ValueError):
        execute_image_resize(
            str(input_path),
            str(output_path),
            0,
            480
        )


def test_missing_input_image(tmp_path):
    input_path = tmp_path / "missing.jpg"
    output_path = tmp_path / "output.jpg"

    with pytest.raises(ValueError):
        execute_image_resize(
            str(input_path),
            str(output_path),
            640,
            480
        )