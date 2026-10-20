import pytest

from edge.executor import execute_sum_squares


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