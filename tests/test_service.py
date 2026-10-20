from edge.service import run_edge_task


def test_run_edge_task_success():
    result = run_edge_task(
        task_id="test_001",
        number_count=4
    )

    assert result["task_id"] == "test_001"
    assert result["execution_location"] == "edge"
    assert result["status"] == "success"
    assert result["task_type"] == "sum_squares"

    assert result["task_input"]["number_count"] == 4

    assert result["result"] == 14
    assert result["execution_time_ms"] >= 0

    assert "cpu_percent" in result["metrics_before"]
    assert "ram_percent" in result["metrics_before"]

    assert "cpu_percent" in result["metrics_after"]
    assert "ram_percent" in result["metrics_after"]

    assert result["error"] is None


def test_run_edge_task_failure():
    result = run_edge_task(
        task_id="test_002",
        number_count=0
    )

    assert result["task_id"] == "test_002"
    assert result["status"] == "failed"
    assert result["result"] is None
    assert result["execution_time_ms"] is None
    assert result["error"] is not None