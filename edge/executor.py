import time
import cv2


def execute_sum_squares(number_count):
    """
    Calculate the sum of squares from 0 up to number_count - 1.

    Example:
        number_count = 4

        0² + 1² + 2² + 3²
        = 0 + 1 + 4 + 9
        = 14

    This gives us a simple CPU task that we can measure.
    """

    if number_count <= 0:
        raise ValueError("number_count must be greater than 0")

    start_time = time.perf_counter()

    result = sum(i * i for i in range(number_count))

    end_time = time.perf_counter()

    execution_time_ms = (end_time - start_time) * 1000

    return {
        "task_type": "sum_squares",
        "number_count": number_count,
        "result": result,
        "execution_time_ms": round(execution_time_ms, 3),
        "status": "success",
    }


if __name__ == "__main__":
    task_result = execute_sum_squares(1_000_000)

    print("\n=== EDGE LOCAL EXECUTION ===\n")
    print(f"Task:             {task_result['task_type']}")
    print(f"Numbers:          {task_result['number_count']}")
    print(f"Result:           {task_result['result']}")
    print(f"Execution Time:   {task_result['execution_time_ms']} ms")
    print(f"Status:           {task_result['status']}")
    print()

def execute_image_resize(input_path, output_path, width, height):
    """
    Resize an image locally on the Edge device.
    """

    if width <= 0 or height <= 0:
        raise ValueError("width and height must be greater than 0")

    image = cv2.imread(input_path)

    if image is None:
        raise ValueError(f"Could not read image: {input_path}")

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
        raise ValueError("Could not save resized image")

    execution_time_ms = (end_time - start_time) * 1000

    return {
        "task_type": "image_resize",
        "input_path": input_path,
        "output_path": output_path,
        "output_width": width,
        "output_height": height,
        "execution_time_ms": round(execution_time_ms, 3),
        "status": "success",
    }