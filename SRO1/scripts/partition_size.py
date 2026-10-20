"""Контроль размера партиции: оценка по формуле и (опционально) факт из nodetool.
   python scripts/partition_size.py
   python scripts/partition_size.py --measure
"""
import argparse
import subprocess

ROW_BYTES = 100            # оценка: ts 8 + 2 double 16 + float 4 + smallint 2 + bool 1 + накладные
SOFT_LIMIT_MB = 10         # NFR3
HARD_LIMIT_MB = 100        # рекомендация Cassandra


def partition_mb(period_s: float, bucket_s: int = 86400) -> float:
    return bucket_s / period_s * ROW_BYTES / 1024 / 1024


def advice(mb: float) -> str:
    if mb <= SOFT_LIMIT_MB:
        return "OK: достаточно (vehicle_id, day)"
    if mb <= HARD_LIMIT_MB:
        return "РИСК: добавить hour в ключ партиции"
    return "НЕЛЬЗЯ: бакет по часу или меньше"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--measure", action="store_true")
    a = ap.parse_args()
    print(f"{'период, с':>10} {'строк/сутки':>12} {'МБ/сутки':>10} {'МБ/час':>8}  вывод")
    for p in (60, 10, 5, 1, 0.5, 0.1):
        mb = partition_mb(p)
        print(f"{p:>10} {int(86400 / p):>12} {mb:>10.2f} {partition_mb(p, 3600):>8.2f}  {advice(mb)}")
    if a.measure:
        cmd = ["docker", "exec", "fleet-cassandra", "nodetool"]
        subprocess.run(cmd + ["flush", "fleet"], check=False)
        out = subprocess.run(cmd + ["tablestats", "fleet.gps_events_by_vehicle_day"],
                             capture_output=True, text=True).stdout
        print("\nФакт (nodetool tablestats):")
        for ln in out.splitlines():
            if "partition" in ln.lower():
                print(" ", ln.strip())


if __name__ == "__main__":
    main()
