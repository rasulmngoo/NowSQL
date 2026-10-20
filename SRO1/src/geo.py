from math import asin, cos, radians, sin, sqrt


def haversine_km(lat1, lon1, lat2, lon2) -> float:
    r = 6371.0088
    dlat, dlon = radians(lat2 - lat1), radians(lon2 - lon1)
    a = sin(dlat / 2) ** 2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon / 2) ** 2
    return 2 * r * asin(sqrt(a))


def track_distance_km(points) -> float:
    """points: итерируемое из (lat, lon) в хронологическом порядке."""
    pts = list(points)
    return sum(haversine_km(*a, *b) for a, b in zip(pts, pts[1:]))
