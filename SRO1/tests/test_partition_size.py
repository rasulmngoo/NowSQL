import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scripts"))
from partition_size import SOFT_LIMIT_MB, partition_mb  # noqa: E402

from src.geo import haversine_km, track_distance_km  # noqa: E402


def test_partition_within_limit_at_1hz_and_not_at_10hz():
    assert partition_mb(5) < SOFT_LIMIT_MB
    assert partition_mb(1) < SOFT_LIMIT_MB
    assert partition_mb(0.1) > SOFT_LIMIT_MB       # 10 Гц требует бакет по часу
    assert partition_mb(0.1, 3600) < SOFT_LIMIT_MB


def test_haversine():
    assert abs(haversine_km(0, 0, 0, 1) - 111.19) < 0.1
    assert track_distance_km([(0, 0)]) == 0
