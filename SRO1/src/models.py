from dataclasses import dataclass
from datetime import date, datetime, timezone


@dataclass(frozen=True)
class Event:
    vehicle_id: str
    event_ts: datetime  # tz-aware, хранится в UTC
    lat: float
    lon: float
    speed_kmh: float
    heading: int = 0
    ignition: bool = True

    @property
    def day(self) -> date:
        """Ключ партиции: календарные сутки по UTC."""
        return self.event_ts.astimezone(timezone.utc).date()
