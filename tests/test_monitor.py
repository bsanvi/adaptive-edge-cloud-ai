from datetime import datetime

from edge.monitor import get_edge_metrics


def test_get_edge_metrics():
    metrics = get_edge_metrics()

    assert "cpu_percent" in metrics
    assert "ram_percent" in metrics
    assert "ram_used_mb" in metrics
    assert "ram_available_mb" in metrics
    assert "battery_percent" in metrics
    assert "power_plugged" in metrics
    assert "timestamp" in metrics

    assert 0 <= metrics["cpu_percent"] <= 100
    assert 0 <= metrics["ram_percent"] <= 100

    assert metrics["ram_used_mb"] >= 0
    assert metrics["ram_available_mb"] >= 0

    if metrics["battery_percent"] is not None:
        assert 0 <= metrics["battery_percent"] <= 100

    datetime.fromisoformat(metrics["timestamp"])