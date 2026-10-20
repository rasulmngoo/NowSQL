# Требования и Access Patterns

## Функциональные
| ID | Требование | Реализация |
|---|---|---|
| FR1 | Регистрация и изменение машины | `Ingestor.upsert_vehicle`, `Queries.update_vehicle` |
| FR2 | Приём GPS-события | `Ingestor.ingest` |
| FR3 | История координат за день | `Queries.track_by_day` |
| FR4 | Выборка за интервал внутри дня | `Queries.interval` |
| FR5 | Последняя позиция машины | `Queries.last_position` |
| FR6 | События превышения скорости за день | `Queries.speeding` |
| FR7 | Суточная сводка за N дней | `Queries.summaries`, `Ingestor.rebuild_summary` |
| FR8 | Удаление суток и автоистечение по TTL | `Queries.delete_day`, `default_time_to_live` |

## Нефункциональные
| ID | Требование | Как проверяется |
|---|---|---|
| NFR1 | Запись ≥ 1000 событий/с на одном узле (демо) | `execute_concurrent_with_args`, замер в отчёте |
| NFR2 | p95 чтения дневной партиции ≤ 50 мс | замер в отчёте |
| NFR3 | Размер партиции ≤ 10 МБ (жёсткий лимит 100 МБ) | `scripts/partition_size.py`, `tests/test_partition_size.py` |
| NFR4 | Хранение 90 дней (TTL), масштабирование добавлением узлов | `default_time_to_live`, TWCS |

## Access Patterns
| № | Паттерн | Приоритет | Ожидаемый результат | Таблица / запрос |
|---|---|---|---|---|
| AP1 | Все точки машины за день | Высокий | Список по убыванию времени | `gps_events_by_vehicle_day` |
| AP2 | Последние N точек за день | Высокий | N строк с головы партиции | то же, `LIMIT N` |
| AP3 | Точки в интервале [t1, t2] | Высокий | Срез по clustering key | то же, `event_ts >= ? AND <= ?` |
| AP4 | Последняя позиция машины | Высокий | Одна строка | `last_position_by_vehicle` |
| AP5 | Трек за несколько дней | Средний | Параллельное чтение партиций, склейка | `track_multi_day` |
| AP6 | Превышения скорости за день | Средний | События нарушений | `speeding_by_vehicle_day` |
| AP7 | Сводки за 30 дней | Средний | 30 строк, новые первыми | `daily_summary_by_vehicle` |
| AP8 | Карточка машины | Низкий | Одна строка | `vehicles` |
| AP9 | Удаление суток / TTL | Средний | Партиция удалена | `DELETE ... WHERE vehicle_id, day` |
| AP10 | Контроль размера партиции | Низкий | Отчёт по размеру | `partition_size.py`, `nodetool` |
