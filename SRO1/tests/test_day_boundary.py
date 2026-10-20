from datetime import date, datetime, timezone

from src.ingest import Ingestor
from src.models import Event
from src.queries import Queries

VID = "TEST-BOUND"


def test_midnight_goes_to_different_partitions(session):
    ing, q = Ingestor(session), Queries(session)
    a = Event(VID, datetime(2026, 2, 1, 23, 59, 59, tzinfo=timezone.utc), 43.2, 76.9, 10)
    b = Event(VID, datetime(2026, 2, 2, 0, 0, 0, tzinfo=timezone.utc), 43.2, 76.9, 10)
    ing.ingest([a, b])
    assert len(q.track_by_day(VID, date(2026, 2, 1))) == 1
    assert len(q.track_by_day(VID, date(2026, 2, 2))) == 1
    for d in (date(2026, 2, 1), date(2026, 2, 2)):
        q.delete_day(VID, d)
