
import pytest

from decision.normalizer import normalize
from decision.normalizer import normalize_metric
from decision.normalizer import desirability_score


def test_normal_cpu():
    result = normalize(80, 0, 100)

    assert result == pytest.approx(0.8)


def test_normal_latency():
    result = normalize(150, 0, 500)

    assert result == pytest.approx(0.3)


def test_value_above_range():
    result = normalize(600, 0, 500)

    assert result == 1.0


def test_value_below_range():
    result = normalize(-20, 0, 100)

    assert result == 0.0


def test_invalid_range():
    with pytest.raises(ValueError):
        normalize(50, 100, 100)


def test_configured_cpu_normalization():
    result = normalize_metric("cpu_percent", 80.0)

    assert result == pytest.approx(0.8)


def test_configured_latency_normalization():
    result = normalize_metric("latency_ms", 150.0)

    assert result == pytest.approx(0.3)


def test_missing_battery_normalization():
    result = normalize_metric("battery_percent", None)

    assert result is None


def test_unknown_metric_rejected():
    with pytest.raises(ValueError):
        normalize_metric("unknown_metric", 50.0)


def test_high_battery_is_good():
    result = desirability_score(
        "battery_percent", 90.0
    )

    assert result == pytest.approx(0.9)


def test_high_cpu_is_bad():
    result = desirability_score(
        "cpu_percent", 80.0
    )

    assert result == pytest.approx(0.2)


def test_low_latency_is_good():
    result = desirability_score(
        "latency_ms", 50.0
    )

    assert result == pytest.approx(0.9)


def test_missing_battery_desirability():
    result = desirability_score(
        "battery_percent", None
    )

    assert result is None
