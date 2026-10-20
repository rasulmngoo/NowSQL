"""Рисует docs/architecture.png: python scripts/draw_architecture.py"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch

boxes = [("Simulator\n(generator.py)", 0.2), ("Ingest\n(ingest.py)", 2.4),
         ("Cassandra 4.1\n(Docker, RF=1)", 4.6), ("Queries / API\n(queries.py, FastAPI)", 6.8)]
fig, ax = plt.subplots(figsize=(10, 3.2))
for text, x in boxes:
    ax.add_patch(FancyBboxPatch((x, 1.0), 1.9, 1.1, boxstyle="round,pad=0.05", fc="#e8f0fe", ec="#1a73e8"))
    ax.text(x + 0.95, 1.55, text, ha="center", va="center", fontsize=9)
for x in (2.1, 4.3, 6.5):
    ax.annotate("", xy=(x + 0.3, 1.55), xytext=(x, 1.55), arrowprops=dict(arrowstyle="->"))
ax.text(5.55, 0.45, "gps_events_by_vehicle_day | last_position | speeding | daily_summary | vehicles",
        ha="center", fontsize=8)
ax.text(5.0, 2.6, "Архитектура: GPS-события транспорта на Cassandra", ha="center", fontsize=11)
ax.set_xlim(0, 9); ax.set_ylim(0, 3); ax.axis("off")
fig.savefig(os.path.join(os.path.dirname(__file__), "..", "docs", "architecture.png"), dpi=150, bbox_inches="tight")
