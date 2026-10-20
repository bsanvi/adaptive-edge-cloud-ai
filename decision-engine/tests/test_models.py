
import pytest
from pydantic import ValidationError

from decision.models import EdgeMetrics


def test_valid_edge_metrics():
    metrics = EdgeMetrics(
        cpu_percent=40,
        ram_percent=55,
        battery_percent=75,
        power_plugged=False
    )

    assert metrics.cpu_percent == 40
    assert metrics.battery_percent == 75


def test_missing_battery():
    metrics = EdgeMetrics(
        cpu_percent=30,
        ram_percent=50
    )

    assert metrics.battery_percent is None


def test_invalid_cpu():
    with pytest.raises(ValidationError):
        EdgeMetrics(
            cpu_percent=150,
            ram_percent=40
        )


def test_invalid_battery():
    with pytest.raises(ValidationError):
        EdgeMetrics(
            cpu_percent=50,
            ram_percent=40,
            battery_percent=-10
        )
