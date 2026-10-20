import os
from dotenv import load_dotenv

load_dotenv()

HOSTS = [h.strip() for h in os.getenv("CASSANDRA_HOSTS", "127.0.0.1").split(",")]
PORT = int(os.getenv("CASSANDRA_PORT", "9042"))
KEYSPACE = os.getenv("CASSANDRA_KEYSPACE", "fleet")
USER = os.getenv("CASSANDRA_USER") or None
PASSWORD = os.getenv("CASSANDRA_PASSWORD") or None
SPEED_LIMIT_KMH = float(os.getenv("SPEED_LIMIT_KMH", "80"))
