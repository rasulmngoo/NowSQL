"""Заполнение БД: 5 машин x 3 дня x 120 событий = 1800 событий.
   python scripts/seed.py            -- запись в Cassandra
   python scripts/seed.py --csv-only -- только data/sample_events.csv (без БД)
"""
import argparse
import csv
import os
import sys
from datetime import datetime, timedelta, timezone

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from src.generator import generate_events  # noqa: E402

VEHICLES = [(f"V{i:03d}", f"{100 + i}KZ05", m, "Almaty-Bus")
            for i, m in enumerate(["MAN Lion", "Yutong E12", "Volvo 7900", "Mercedes Citaro", "Ikarus"], 1)]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv-only", action="store_true")
    ap.add_argument("--days", type=int, default=3)
    ap.add_argument("--points", type=int, default=120)
    a = ap.parse_args()
    today = datetime.now(timezone.utc).date()
    days = [today - timedelta(days=i) for i in range(a.days)]
    events = [e for v in VEHICLES for d in days for e in generate_events(v[0], d, a.points)]

    csv_path = os.path.join(os.path.dirname(__file__), "..", "data", "sample_events.csv")
    with open(csv_path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["vehicle_id", "event_ts", "lat", "lon", "speed_kmh", "heading", "ignition"])
        for e in events:
            w.writerow([e.vehicle_id, e.event_ts.isoformat(), e.lat, e.lon,
                        e.speed_kmh, e.heading, e.ignition])
    print(f"CSV: {len(events)} событий -> data/sample_events.csv")
    if a.csv_only:
        return

    from src.db import get_session
    from src.ingest import Ingestor
    cluster, session = get_session()
    ing = Ingestor(session)
    for v in VEHICLES:
        ing.upsert_vehicle(*v)
    print("Записано событий:", ing.ingest(events))
    for v in VEHICLES:
        for d in days:
            ing.rebuild_summary(v[0], d)
    print("Сводки пересчитаны. Готово.")
    cluster.shutdown()


if __name__ == "__main__":
    main()
