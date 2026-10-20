import os
import sys

import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))


@pytest.fixture(scope="session")
def session():
    from src.db import get_session
    try:
        cluster, s = get_session()
    except Exception as exc:  # Cassandra не запущена
        pytest.skip(f"Cassandra недоступна: {exc}")
    yield s
    cluster.shutdown()
