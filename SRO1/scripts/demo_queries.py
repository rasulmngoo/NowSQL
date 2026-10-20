"""10 демонстрационных запросов (8 простых + 2 нетривиальных). Сначала: python scripts/seed.py"""
import os
import sys
from datetime import datetime, timedelta, timezone

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from src.db import get_session  # noqa: E402
from src.queries import Queries  # noqa: E402

V = "V001"


def show(title, data):
    print(f"\n=== {title}")
    print(data)


def main():
    cluster, s = get_session()
    q = Queries(s)
    today = datetime.now(timezone.utc).date()
    d0, days = today, [today - timedelta(days=i) for i in range(3)]

    track = q.track_by_day(V, d0)
    show(f"Q1 (AP1) Трек {V} за {d0}: {len(track)} точек, первая = самая новая", track[:2])
    show("Q2 (AP2) Последние 5 точек", [(r.event_ts, r.lat, r.lon) for r in q.last_n(V, d0, 5)])
    t1 = datetime(d0.year, d0.month, d0.day, 8, tzinfo=timezone.utc)
    iv = q.interval(V, d0, t1, t1 + timedelta(hours=1))
    show("Q3 (AP3) Интервал 08:00-09:00 UTC", f"{len(iv)} точек")
    show("Q4 (AP4) Последняя позиция", q.last_position(V))
    sp = q.speeding(V, d0)
    show("Q5 (AP6) Превышения скорости за день", f"{len(sp)} событий; пример: {sp[:2]}")
    show("Q6 (AP7) Сводки за 30 дней", q.summaries(V, 30))
    show("Q7 Агрегаты count/max/avg по партиции", q.day_stats(V, d0))
    q.update_vehicle("V002", "NEW123KZ", "Yutong E13")
    show("Q8 (AP8) UPDATE справочника + чтение", q.get_vehicle("V002"))
    rows, dist = q.track_multi_day(V, days)
    show("Q9 (нетривиальный) Трек за 3 дня: параллельные чтения + haversine",
         f"{len(rows)} точек, пробег {dist:.1f} км")
    n, pages = q.paged_count(V, d0, fetch_size=25)
    show("Q10 (нетривиальный) Постраничное чтение партиции (fetch_size=25)",
         f"{n} строк за {pages} стр.")
    cluster.shutdown()


if __name__ == "__main__":
    main()
