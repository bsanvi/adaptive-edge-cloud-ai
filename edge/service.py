from edge.executor import execute_image_resize
from edge.monitor import get_edge_metrics


def run_edge_task(
    task_id,
    input_path,
    output_path,
    width,
    height
):
    """
    Execute an image-resize task locally on the Edge device.

    The service:
    1. Measures device metrics before execution.
    2. Executes the image resize.
    3. Measures device metrics after execution.
    4. Returns one structured result.
    """

    metrics_before = get_edge_metrics()

    try:
        execution = execute_image_resize(
            input_path=input_path,
            output_path=output_path,
            width=width,
            height=height
        )

        status = "success"
        execution_time_ms = execution["execution_time_ms"]
        result = execution
        error = None

    except Exception as exc:
        status = "failed"
        execution_time_ms = None
        result = None
        error = str(exc)

    metrics_after = get_edge_metrics()

    return {
        "task_id": task_id,
        "execution_location": "edge",
        "status": status,
        "task_type": "image_resize",

        "task_input": {
            "input_path": input_path,
            "output_path": output_path,
            "width": width,
            "height": height
        },

        "execution_time_ms": execution_time_ms,

        "metrics_before": metrics_before,
        "metrics_after": metrics_after,

        "result": result,
        "error": error
    }

def print_edge_task_result(task_result):
    """
    Print an Edge task result in a human-readable format.
    """

    print("\n" + "=" * 60)
    print("                 EDGE TASK RESULT")
    print("=" * 60)

    print(f"\nTask ID:             {task_result['task_id']}")
    print(f"Execution Location:  {task_result['execution_location']}")
    print(f"Task Type:           {task_result['task_type']}")
    print(f"Status:              {task_result['status']}")

    print("\n--- TASK INPUT ---")
    print(f"Input Path:          {task_result['task_input']['input_path']}")
    print(f"Output Path:         {task_result['task_input']['output_path']}")
    print(f"Requested Width:     {task_result['task_input']['width']}")
    print(f"Requested Height:    {task_result['task_input']['height']}")

    print("\n--- DEVICE BEFORE EXECUTION ---")
    before = task_result["metrics_before"]

    print(f"CPU Usage:           {before['cpu_percent']} %")
    print(f"RAM Usage:           {before['ram_percent']} %")
    print(f"RAM Used:            {before['ram_used_mb']} MB")
    print(f"RAM Available:       {before['ram_available_mb']} MB")
    print(f"Battery:             {before['battery_percent']} %")
    print(f"Power Plugged:       {before['power_plugged']}")
    print(f"Timestamp:           {before['timestamp']}")

    if task_result["status"] == "success":
        result = task_result["result"]

        print("\n--- IMAGE PROCESSING RESULT ---")

        print(
            f"Input Dimensions:    "
            f"{result['input_width']} x {result['input_height']}"
        )

        print(
            f"Output Dimensions:   "
            f"{result['output_width']} x {result['output_height']}"
        )

        print(f"Input Size:          {result['input_size_bytes']} bytes")
        print(f"Output Size:         {result['output_size_bytes']} bytes")

        print(
            f"Size Reduction:      "
            f"{result['size_reduction_percent']} %"
        )

        print(
            f"Execution Time:      "
            f"{result['execution_time_ms']} ms"
        )

    else:
        print("\n--- ERROR ---")
        print(task_result["error"])

    print("\n--- DEVICE AFTER EXECUTION ---")
    after = task_result["metrics_after"]

    print(f"CPU Usage:           {after['cpu_percent']} %")
    print(f"RAM Usage:           {after['ram_percent']} %")
    print(f"RAM Used:            {after['ram_used_mb']} MB")
    print(f"RAM Available:       {after['ram_available_mb']} MB")
    print(f"Battery:             {after['battery_percent']} %")
    print(f"Power Plugged:       {after['power_plugged']}")
    print(f"Timestamp:           {after['timestamp']}")

    print("\n" + "=" * 60)
    print()