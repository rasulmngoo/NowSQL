import os
from cassandra.auth import PlainTextAuthProvider
from cassandra.cluster import Cluster

from . import config

SCHEMA_PATH = os.path.join(os.path.dirname(__file__), "..", "scripts", "init_schema.cql")


def init_schema(session) -> None:
    """Выполняет scripts/init_schema.cql (идемпотентно: IF NOT EXISTS)."""
    with open(SCHEMA_PATH, encoding="utf-8") as f:
        lines = [ln.split("--")[0] for ln in f]
    for stmt in "\n".join(lines).split(";"):
        if stmt.strip():
            session.execute(stmt)


def get_session(init: bool = True):
    auth = None
    if config.USER:
        auth = PlainTextAuthProvider(config.USER, config.PASSWORD)
    cluster = Cluster(config.HOSTS, port=config.PORT, auth_provider=auth)
    session = cluster.connect()
    if init:
        init_schema(session)
    session.set_keyspace(config.KEYSPACE)
    return cluster, session
