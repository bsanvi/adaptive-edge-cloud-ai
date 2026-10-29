
import random

from experiments.baselines import (
    always_edge,
    always_cloud,
    random_location,
    LOCATIONS,
)


def test_always_edge():
    assert always_edge() == "edge"


def test_always_cloud():
    assert always_cloud() == "cloud"


def test_random_selects_valid_location():
    rng = random.Random(42)

    for _ in range(100):
        location = random_location(rng)

        assert location in LOCATIONS


def test_random_is_reproducible():
    first_rng = random.Random(42)
    second_rng = random.Random(42)

    first_sequence = [
        random_location(first_rng)
        for _ in range(20)
    ]

    second_sequence = [
        random_location(second_rng)
        for _ in range(20)
    ]

    assert first_sequence == second_sequence
