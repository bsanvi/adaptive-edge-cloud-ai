from edge.executor import execute_sum_squares
from edge.monitor import get_edge_metrics


def run_edge_task(task_id, number_count):
    """
    Execute a task locally on the Edge device and collect
    system metrics before and after execution.
    """

    metrics_before = get_edge_metrics()

    try:
        execution = execute_sum_squares(number_count)

        status = "success"
        result = execution["result"]
        execution_time_ms = execution["execution_time_ms"]
        error = None

    except Exception as exc:
        status = "failed"
        result = None
        execution_time_ms = None
        error = str(exc)

    metrics_after = get_edge_metrics()

    return {
        "task_id": task_id,
        "execution_location": "edge",
        "status": status,
        "task_type": "sum_squares",
        "task_input": {
            "number_count": number_count
        },
        "execution_time_ms": execution_time_ms,
        "metrics_before": metrics_before,
        "metrics_after": metrics_after,
        "result": result,
        "error": error,
    }


def print_edge_task_result(task_result):
    print("\n=== EDGE TASK RESULT ===\n")

    print(f"Task ID:            {task_result['task_id']}")
    print(f"Execution Location: {task_result['execution_location']}")
    print(f"Task Type:          {task_result['task_type']}")
    print(f"Status:             {task_result['status']}")

    if task_result["status"] == "success":
        print(f"Execution Time:     {task_result['execution_time_ms']} ms")
        print(f"Result:             {task_result['result']}")
    else:
        print(f"Error:              {task_result['error']}")

    print("\n--- BEFORE EXECUTION ---")
    print(f"CPU: {task_result['metrics_before']['cpu_percent']} %")
    print(f"RAM: {task_result['metrics_before']['ram_percent']} %")
    print(f"Battery: {task_result['metrics_before']['battery_percent']} %")

    print("\n--- AFTER EXECUTION ---")
    print(f"CPU: {task_result['metrics_after']['cpu_percent']} %")
    print(f"RAM: {task_result['metrics_after']['ram_percent']} %")
    print(f"Battery: {task_result['metrics_after']['battery_percent']} %")

    print()


if __name__ == "__main__":
    task_result = run_edge_task(
        task_id="task_001",
        number_count=1_000_000
    )

    print_edge_task_result(task_result)