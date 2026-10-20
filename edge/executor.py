import os
import time

import cv2


def execute_image_resize(input_path, output_path, width, height):
    """
    Resize an image locally on the Edge device.

    The function also measures:
    - input image dimensions
    - output image dimensions
    - input file size
    - output file size
    - size reduction percentage
    - local execution time
    """

    if width <= 0 or height <= 0:
        raise ValueError("width and height must be greater than 0")

    if not os.path.exists(input_path):
        raise ValueError(f"Input file does not exist: {input_path}")

    # Measure original file size.
    input_size_bytes = os.path.getsize(input_path)

    # Read image from disk.
    image = cv2.imread(input_path)

    if image is None:
        raise ValueError(f"Could not read image: {input_path}")

    # OpenCV stores image shape as:
    # height, width, channels
    input_height, input_width = image.shape[:2]

    # Measure only the actual resize/save operation.
    start_time = time.perf_counter()

    resized_image = cv2.resize(
        image,
        (width, height)
    )

    success = cv2.imwrite(
        output_path,
        resized_image
    )

    end_time = time.perf_counter()

    if not success:
        raise ValueError(f"Could not save image: {output_path}")

    execution_time_ms = (end_time - start_time) * 1000

    # Measure output file size.
    output_size_bytes = os.path.getsize(output_path)

    size_reduction_percent = (
        (input_size_bytes - output_size_bytes)
        / input_size_bytes
        * 100
    )

    return {
        "task_type": "image_resize",

        "input_path": input_path,
        "output_path": output_path,

        "input_width": input_width,
        "input_height": input_height,

        "output_width": width,
        "output_height": height,

        "input_size_bytes": input_size_bytes,
        "output_size_bytes": output_size_bytes,

        "size_reduction_percent": round(
            size_reduction_percent,
            2
        ),

        "execution_time_ms": round(
            execution_time_ms,
            3
        ),

        "status": "success",
    }