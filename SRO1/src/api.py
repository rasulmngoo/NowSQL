"""Минимальный REST-интерфейс. Запуск: uvicorn src.api:app --reload"""
from datetime import date, datetime

from fastapi import FastAPI, HTTPException

from .db import get_session
from .queries import Queries

app = FastAPI(title="Fleet GPS (Cassandra)")
_cluster, _session = get_session()
q = Queries(_session)


def _d(rows):
    return [dict(r._asdict()) for r in rows]


@app.get("/vehicles/{vid}/track")
def track(vid: str, day: date, limit: int | None = None):
    return _d(q.last_n(vid, day, limit) if limit else q.track_by_day(vid, day))


@app.get("/vehicles/{vid}/interval")
def interval(vid: str, day: date, t1: datetime, t2: datetime):
    return _d(q.interval(vid, day, t1, t2))


@app.get("/vehicles/{vid}/last")
def last(vid: str):
    r = q.last_position(vid)
    if not r:
        raise HTTPException(404, "нет данных")
    return r._asdict()


@app.get("/vehicles/{vid}/summary")
def summary(vid: str, days: int = 30):
    return _d(q.summaries(vid, days))
