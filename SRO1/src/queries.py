from datetime import timedelta

from cassandra.concurrent import execute_concurrent_with_args
from cassandra.query import SimpleStatement

from .geo import track_distance_km

COLS = "event_ts, lat, lon, speed_kmh, heading, ignition"
T = "gps_events_by_vehicle_day"


class Queries:
    """Читающие запросы: каждый соответствует одному Access Pattern."""

    def __init__(self, session):
        s = self.s = session
        self.p_track = s.prepare(f"SELECT {COLS} FROM {T} WHERE vehicle_id=? AND day=?")
        self.p_limit = s.prepare(f"SELECT {COLS} FROM {T} WHERE vehicle_id=? AND day=? LIMIT ?")
        self.p_interval = s.prepare(
            f"SELECT {COLS} FROM {T} WHERE vehicle_id=? AND day=? AND event_ts>=? AND event_ts<=?")
        self.p_last = s.prepare("SELECT * FROM last_position_by_vehicle WHERE vehicle_id=?")
        self.p_speeding = s.prepare(
            "SELECT event_ts, lat, lon, speed_kmh FROM speeding_by_vehicle_day "
            "WHERE vehicle_id=? AND day=?")
        self.p_summ = s.prepare(
            "SELECT day, points, max_speed, distance_km FROM daily_summary_by_vehicle "
            "WHERE vehicle_id=? LIMIT ?")
        self.p_stats = s.prepare(
            f"SELECT count(*) AS n, max(speed_kmh) AS max_speed, avg(speed_kmh) AS avg_speed "
            f"FROM {T} WHERE vehicle_id=? AND day=?")
        self.p_vehicle = s.prepare("SELECT * FROM vehicles WHERE vehicle_id=?")
        self.p_upd_vehicle = s.prepare("UPDATE vehicles SET plate=?, model=? WHERE vehicle_id=?")
        self.p_del_vehicle = s.prepare("DELETE FROM vehicles WHERE vehicle_id=?")
        self.p_del = [
            s.prepare(f"DELETE FROM {T} WHERE vehicle_id=? AND day=?"),
            s.prepare("DELETE FROM speeding_by_vehicle_day WHERE vehicle_id=? AND day=?"),
            s.prepare("DELETE FROM daily_summary_by_vehicle WHERE vehicle_id=? AND day=?"),
        ]

    # AP1: трек за день, новые первыми (порядок задан CLUSTERING ORDER)
    def track_by_day(self, vehicle_id, day):
        return list(self.s.execute(self.p_track, (vehicle_id, day)))

    # AP2: последние N точек дня — чтение с головы партиции
    def last_n(self, vehicle_id, day, n):
        return list(self.s.execute(self.p_limit, (vehicle_id, day, n)))

    # AP3: срез по clustering key
    def interval(self, vehicle_id, day, t1, t2):
        return list(self.s.execute(self.p_interval, (vehicle_id, day, t1, t2)))

    # AP4
    def last_position(self, vehicle_id):
        return self.s.execute(self.p_last, (vehicle_id,)).one()

    # AP5 (нетривиальный): параллельное чтение партиций по дням + пробег
    def track_multi_day(self, vehicle_id, days):
        res = execute_concurrent_with_args(
            self.s, self.p_track, [(vehicle_id, d) for d in days], concurrency=10)
        rows = []
        for ok, r in res:
            if not ok:
                raise r
            rows.extend(r)
        rows.sort(key=lambda x: x.event_ts)
        return rows, track_distance_km((r.lat, r.lon) for r in rows)

    # AP6
    def speeding(self, vehicle_id, day):
        return list(self.s.execute(self.p_speeding, (vehicle_id, day)))

    # AP7
    def summaries(self, vehicle_id, n=30):
        return list(self.s.execute(self.p_summ, (vehicle_id, n)))

    # Агрегаты внутри одной партиции (допустимо: ограниченная партиция)
    def day_stats(self, vehicle_id, day):
        return self.s.execute(self.p_stats, (vehicle_id, day)).one()

    # AP10 (нетривиальный): постраничное чтение большой партиции
    def paged_count(self, vehicle_id, day, fetch_size=100):
        stmt = SimpleStatement(
            f"SELECT event_ts FROM {T} WHERE vehicle_id=%s AND day=%s", fetch_size=fetch_size)
        rs = self.s.execute(stmt, (vehicle_id, day))
        pages = rows = 0
        while True:
            rows += len(rs.current_rows)
            pages += 1
            if not rs.has_more_pages:
                break
            rs.fetch_next_page()
        return rows, pages

    # AP8: чтение / UPDATE / DELETE справочника
    def get_vehicle(self, vehicle_id):
        return self.s.execute(self.p_vehicle, (vehicle_id,)).one()

    def update_vehicle(self, vehicle_id, plate, model):
        self.s.execute(self.p_upd_vehicle, (plate, model, vehicle_id))

    def delete_vehicle(self, vehicle_id):
        self.s.execute(self.p_del_vehicle, (vehicle_id,))

    # AP9: удаление суток целиком (tombstone на партицию)
    def delete_day(self, vehicle_id, day):
        for stmt in self.p_del:
            self.s.execute(stmt, (vehicle_id, day))

    def ttl_of(self, vehicle_id, day):
        r = self.s.execute(
            f"SELECT TTL(lat) AS ttl FROM {T} WHERE vehicle_id=%s AND day=%s LIMIT 1",
            (vehicle_id, day)).one()
        return r.ttl if r else None
