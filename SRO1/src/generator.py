import random
from datetime import datetime, time, timedelta, timezone

from .models import Event

ALMATY = (43.238, 76.945)


def generate_events(vehicle_id, day, n=120, seed=0):
    """Синтетический трек: случайное блуждание вокруг Алматы, n точек равномерно по суткам UTC."""
    rnd = random.Random(f"{vehicle_id}-{day}-{seed}")
    lat = ALMATY[0] + rnd.uniform(-0.05, 0.05)
    lon = ALMATY[1] + rnd.uniform(-0.05, 0.05)
    start = datetime.combine(day, time.min, tzinfo=timezone.utc)
    step = 86400 / n
    events = []
    for i in range(n):
        lat += rnd.uniform(-0.002, 0.002)
        lon += rnd.uniform(-0.002, 0.002)
        speed = round(rnd.uniform(0, 70) + (rnd.uniform(15, 40) if rnd.random() < 0.08 else 0), 1)
        events.append(Event(vehicle_id, start + timedelta(seconds=i * step),
                            round(lat, 6), round(lon, 6), speed,
                            rnd.randint(0, 359), speed > 0))
    return events
