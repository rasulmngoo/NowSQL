from datetime import date

from src.generator import generate_events
from src.ingest import Ingestor
from src.queries import Queries

VID, DAY = "TEST-ORDER", date(2026, 1, 15)


def test_clustering_order_desc(session):
    ing, q = Ingestor(session), Queries(session)
    ing.ingest(generate_events(VID, DAY, n=50))
    rows = q.track_by_day(VID, DAY)
    assert len(rows) == 50
    ts = [r.event_ts for r in rows]
    assert ts == sorted(ts, reverse=True)           # новые первыми
    assert q.last_n(VID, DAY, 5)[0].event_ts == max(ts)
    q.delete_day(VID, DAY)
