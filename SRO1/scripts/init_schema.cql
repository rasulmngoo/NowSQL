CREATE KEYSPACE IF NOT EXISTS fleet WITH replication =
  {'class': 'SimpleStrategy', 'replication_factor': 1};

USE fleet;

-- AP8: справочник машин
CREATE TABLE IF NOT EXISTS vehicles (
  vehicle_id text PRIMARY KEY,
  plate text,
  model text,
  fleet text
);

-- AP1, AP2, AP3, AP5: основная таблица. Партиция = машина + сутки (UTC).
CREATE TABLE IF NOT EXISTS gps_events_by_vehicle_day (
  vehicle_id text,
  day        date,
  event_ts   timestamp,
  lat        double,
  lon        double,
  speed_kmh  float,
  heading    smallint,
  ignition   boolean,
  PRIMARY KEY ((vehicle_id, day), event_ts)
) WITH CLUSTERING ORDER BY (event_ts DESC)
  AND default_time_to_live = 7776000
  AND compaction = {'class': 'TimeWindowCompactionStrategy',
                    'compaction_window_unit': 'DAYS',
                    'compaction_window_size': 1};

-- AP4: последняя позиция (LWW по USING TIMESTAMP = время события)
CREATE TABLE IF NOT EXISTS last_position_by_vehicle (
  vehicle_id text PRIMARY KEY,
  event_ts timestamp,
  lat double,
  lon double,
  speed_kmh float
);

-- AP6: превышения скорости
CREATE TABLE IF NOT EXISTS speeding_by_vehicle_day (
  vehicle_id text,
  day date,
  event_ts timestamp,
  lat double,
  lon double,
  speed_kmh float,
  PRIMARY KEY ((vehicle_id, day), event_ts)
) WITH CLUSTERING ORDER BY (event_ts DESC)
  AND default_time_to_live = 7776000;

-- AP7: суточные сводки
CREATE TABLE IF NOT EXISTS daily_summary_by_vehicle (
  vehicle_id text,
  day date,
  points int,
  max_speed float,
  distance_km double,
  PRIMARY KEY ((vehicle_id), day)
) WITH CLUSTERING ORDER BY (day DESC);
