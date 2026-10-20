from datetime import date

from src.generator import generate_events
from src.ingest import Ingestor
from src.queries import Queries

VID, DAY = "TEST-CRUD", date(2026, 3, 10)


def test_crud_and_ttl(session):
    ing, q = Ingestor(session), Queries(session)
    ing.upsert_vehicle(VID, "111KZ", "Model A", "T")                      # Create
    assert q.get_vehicle(VID).plate == "111KZ"                            # Read
    q.update_vehicle(VID, "222KZ", "Model B")                             # Update
    assert q.get_vehicle(VID).model == "Model B"
    ing.ingest(generate_events(VID, DAY, n=30))
    ttl = q.ttl_of(VID, DAY)
    assert ttl is not None and 0 < ttl <= 7776000                         # default TTL 90 дней
    q.delete_day(VID, DAY)                                                # Delete партиции
    assert q.track_by_day(VID, DAY) == []
    q.delete_vehicle(VID)
    assert q.get_vehicle(VID) is None


def test_idempotent_upsert(session):
    ing, q = Ingestor(session), Queries(session)
    ev = generate_events("TEST-IDEM", DAY, n=10)
    ing.ingest(ev)
    ing.ingest(ev)                                                        # повтор не дублирует
    assert len(q.track_by_day("TEST-IDEM", DAY)) == 10
    q.delete_day("TEST-IDEM", DAY)
