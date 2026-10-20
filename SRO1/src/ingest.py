from cassandra.concurrent import execute_concurrent_with_args

from . import config
from .geo import track_distance_km


class Ingestor:
    """Запись событий в денормализованные таблицы (CREATE/UPDATE)."""

    def __init__(self, session, speed_limit: float = config.SPEED_LIMIT_KMH):
        self.s = session
        self.limit = speed_limit
        self.p_gps = session.prepare(
            "INSERT INTO gps_events_by_vehicle_day "
            "(vehicle_id, day, event_ts, lat, lon, speed_kmh, heading, ignition) "
            "VALUES (?,?,?,?,?,?,?,?)")
        self.p_speed = session.prepare(
            "INSERT INTO speeding_by_vehicle_day "
            "(vehicle_id, day, event_ts, lat, lon, speed_kmh) VALUES (?,?,?,?,?,?)")
        # USING TIMESTAMP = время события: запоздавшее событие не затрёт более новое (LWW)
        self.p_last = session.prepare(
            "INSERT INTO last_position_by_vehicle "
            "(vehicle_id, event_ts, lat, lon, speed_kmh) VALUES (?,?,?,?,?) USING TIMESTAMP ?")
        self.p_vehicle = session.prepare(
            "INSERT INTO vehicles (vehicle_id, plate, model, fleet) VALUES (?,?,?,?)")
        self.p_summary = session.prepare(
            "INSERT INTO daily_summary_by_vehicle "
            "(vehicle_id, day, points, max_speed, distance_km) VALUES (?,?,?,?,?)")
        self.p_read_day = session.prepare(
            "SELECT lat, lon, speed_kmh FROM gps_events_by_vehicle_day "
            "WHERE vehicle_id=? AND day=? ORDER BY event_ts ASC")

    def _run(self, stmt, params, concurrency=100):
        if params:
            execute_concurrent_with_args(self.s, stmt, params, concurrency=concurrency)

    def ingest(self, events) -> int:
        """Идемпотентный upsert: повтор того же (vehicle, day, event_ts) перезапишет строку."""
        events = list(events)
        self._run(self.p_gps, [(e.vehicle_id, e.day, e.event_ts, e.lat, e.lon,
                                e.speed_kmh, e.heading, e.ignition) for e in events])
        self._run(self.p_speed, [(e.vehicle_id, e.day, e.event_ts, e.lat, e.lon, e.speed_kmh)
                                 for e in events if e.speed_kmh > self.limit])
        latest = {}
        for e in events:
            if e.vehicle_id not in latest or e.event_ts > latest[e.vehicle_id].event_ts:
                latest[e.vehicle_id] = e
        self._run(self.p_last, [(e.vehicle_id, e.event_ts, e.lat, e.lon, e.speed_kmh,
                                 int(e.event_ts.timestamp() * 1_000_000))
                                for e in latest.values()])
        return len(events)

    def upsert_vehicle(self, vehicle_id, plate, model, fleet):
        self.s.execute(self.p_vehicle, (vehicle_id, plate, model, fleet))

    def rebuild_summary(self, vehicle_id, day):
        """Пересчёт суточной сводки по партиции (поддерживается приложением)."""
        rows = list(self.s.execute(self.p_read_day, (vehicle_id, day)))
        if not rows:
            return None
        dist = track_distance_km((r.lat, r.lon) for r in rows)
        mx = max(r.speed_kmh for r in rows)
        self.s.execute(self.p_summary, (vehicle_id, day, len(rows), mx, dist))
        return len(rows), mx, dist
