
import random

from decision.models import Location


LOCATIONS = [
    Location.EDGE.value,
    Location.CLOUD.value,
    Location.HYBRID.value,
]


def always_edge() -> str:
    return Location.EDGE.value


def always_cloud() -> str:
    return Location.CLOUD.value


def random_location(rng: random.Random) -> str:
    return rng.choice(LOCATIONS)
