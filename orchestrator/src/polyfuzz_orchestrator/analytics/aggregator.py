"""Cross-campaign statistics and time-grid interpolation for growth curves.

Aggregates per-campaign metrics into summary statistics with 95% confidence intervals using the t-distribution,
and interpolates time-series data to a common adaptive time grid for cross-campaign growth curve analysis.
"""

from __future__ import annotations

import math
import statistics
from bisect import bisect_right
from dataclasses import dataclass, field

from scipy.stats import t as t_dist

from polyfuzz_orchestrator.analytics.metrics import CampaignMetrics

# Numeric metric fields from CampaignMetrics to aggregate.
# Excludes: campaign_id (str), seed (identifier, not a metric), plot_data (list).
NUMERIC_METRIC_FIELDS: list[str] = [
    "bitmap_cvg",
    "edges_found",
    "mismatch_count",
    "mismatch_rate",
    "corpus_initial",
    "corpus_final",
    "corpus_growth_pct",
    "total_time_s",
    "stage_smlgen_s",
    "stage_afl_s",
    "stage_diffcomp_s",
    "stage_coverage_s",
    "branch_coverage_pct",
]

# Growth curve metrics: output name -> plot_data column name.
GROWTH_METRICS: dict[str, str] = {
    "edges_found": "edges_found",
    "bitmap_cvg": "map_size",
    "total_execs": "total_execs",
    "total_crashes": "total_crashes",
    "corpus_count": "corpus_count",
}


@dataclass
class GrowthCurveData:
    """Aggregated growth curve data for a single metric across campaigns.

    Attributes:
        time_grid: Common time grid points in seconds.
        per_campaign: Mapping of campaign_id to interpolated values at each grid point.
        mean: Mean value across campaigns at each grid point.
        ci_lower: Lower bound of 95% CI at each grid point.
        ci_upper: Upper bound of 95% CI at each grid point.
        campaign_count: Number of campaigns with data at each grid point.
    """

    time_grid: list[float] = field(default_factory=list)
    per_campaign: dict[str, list[float]] = field(default_factory=dict)
    mean: list[float] = field(default_factory=list)
    ci_lower: list[float] = field(default_factory=list)
    ci_upper: list[float] = field(default_factory=list)
    campaign_count: list[int] = field(default_factory=list)


def compute_metric_stats(values: list[float]) -> dict[str, float]:
    """Compute summary statistics for a list of metric values.

    Calculates mean, median, standard deviation, and 95% confidence interval
    using the Student's t-distribution.
    Args:
        values: List of numeric values from each campaign.
    Returns:
        Dictionary with keys: mean, median, stdev, ci_lower, ci_upper.
    """
    n = len(values)

    if n == 0:
        return {"mean": 0.0, "median": 0.0, "stdev": 0.0, "ci_lower": 0.0, "ci_upper": 0.0}

    if n == 1:
        v = values[0]
        return {"mean": v, "median": v, "stdev": 0.0, "ci_lower": v, "ci_upper": v}

    m = statistics.mean(values)
    med = statistics.median(values)
    sd = statistics.stdev(values)

    if sd == 0.0:
        return {"mean": m, "median": med, "stdev": 0.0, "ci_lower": m, "ci_upper": m}

    se = sd / math.sqrt(n)
    ci_low, ci_high = t_dist.interval(confidence=0.95, df=n - 1, loc=m, scale=se)

    return {
        "mean": m,
        "median": med,
        "stdev": sd,
        "ci_lower": float(ci_low),
        "ci_upper": float(ci_high),
    }


def compute_cross_campaign_stats(
    metrics: list[CampaignMetrics],
) -> dict[str, dict[str, float]]:
    """Compute summary statistics across campaigns for each numeric metric.
    Args:
        metrics: List of CampaignMetrics from completed campaigns.
    Returns:
        Dictionary mapping metric field name to stats dict
        (mean, median, stdev, ci_lower, ci_upper).
    """
    result: dict[str, dict[str, float]] = {}
    for field_name in NUMERIC_METRIC_FIELDS:
        values = [float(getattr(m, field_name)) for m in metrics]
        result[field_name] = compute_metric_stats(values)
    return result


def build_time_grid(max_duration_s: float) -> list[float]:
    """Build an adaptive time grid from 0 to max_duration_s.
    Step size adapts to duration:
    - <=60s: 1s step
    - <=600s: 5s step
    - <=3600s: 10s step
    - >3600s: 30s step
    Args:
        max_duration_s: Maximum duration in seconds.
    Returns:
        List of time points from 0.0 to max_duration_s (inclusive).
    """
    if max_duration_s <= 0:
        return [0.0]

    if max_duration_s <= 60:
        step = 1.0
    elif max_duration_s <= 600:
        step = 5.0
    elif max_duration_s <= 3600:
        step = 10.0
    else:
        step = 30.0

    grid: list[float] = []
    t = 0.0
    while t <= max_duration_s:
        grid.append(round(t, 1))
        t += step

    # Ensure the last point is included
    if not grid or grid[-1] < max_duration_s:
        grid.append(round(max_duration_s, 1))

    return grid


def interpolate_at(times: list[float], values: list[float], target_t: float) -> float:
    """Linearly interpolate a value at target_t, clamping to boundary values.

    Uses bisect for efficient lookup. If target_t is outside the range of times, returns the nearest
    boundary value (forward-fill).
    Args:
        times: Sorted list of time points.
        values: Values corresponding to each time point.
        target_t: Time at which to interpolate.
    Returns:
        Interpolated (or clamped) value at target_t.
    """
    if len(times) == 0:
        return 0.0

    if len(times) == 1:
        return values[0]

    # Clamp to boundaries
    if target_t <= times[0]:
        return values[0]
    if target_t >= times[-1]:
        return values[-1]

    # Find the right bracket
    idx = bisect_right(times, target_t)
    # idx is the index of the first element > target_t
    # So times[idx-1] <= target_t < times[idx]
    t0, t1 = times[idx - 1], times[idx]
    v0, v1 = values[idx - 1], values[idx]

    if t1 == t0:
        return v0

    frac = (target_t - t0) / (t1 - t0)
    return v0 + frac * (v1 - v0)


def interpolate_growth_curves(
    metrics: list[CampaignMetrics],
) -> dict[str, GrowthCurveData]:
    """Interpolate growth curves across campaigns to a common time grid.

    For each tracked metric, interpolates each campaign's time-series data to a common adaptive time grid,
    then computes per-point mean and 95% CI. Campaigns with empty plot_data are excluded. Short campaigns are
    forward-filled with their last known value.
    Args:
        metrics: List of CampaignMetrics with plot_data populated.
    Returns:
        Dictionary mapping metric name to GrowthCurveData.
        Empty dict if no campaigns have plot_data.
    """
    # Filter campaigns with non-empty plot_data
    active = [m for m in metrics if m.plot_data]
    if not active:
        return {}

    # Determine max duration across all campaigns
    max_duration = max(m.plot_data[-1]["relative_time"] for m in active)
    if max_duration <= 0:
        return {}

    time_grid = build_time_grid(max_duration)

    # Track each campaign's max time for campaign_count
    campaign_max_times = {m.campaign_id: m.plot_data[-1]["relative_time"] for m in active}

    result: dict[str, GrowthCurveData] = {}

    for metric_name, plot_col in GROWTH_METRICS.items():
        curve = GrowthCurveData(time_grid=time_grid)

        # Interpolate each campaign
        for m in active:
            times_raw = [row["relative_time"] for row in m.plot_data]
            values_raw = [row.get(plot_col, 0.0) for row in m.plot_data]

            interpolated = [interpolate_at(times_raw, values_raw, t) for t in time_grid]
            curve.per_campaign[m.campaign_id] = interpolated

        # Compute per-grid-point statistics and campaign_count
        campaign_ids = sorted(curve.per_campaign.keys())
        for i, t in enumerate(time_grid):
            # Count campaigns whose max time >= this grid point
            count = sum(1 for cid in campaign_ids if campaign_max_times[cid] >= t)
            curve.campaign_count.append(count)

            # Gather values from all campaigns at this grid point
            point_values = [curve.per_campaign[cid][i] for cid in campaign_ids]
            stats = compute_metric_stats(point_values)
            curve.mean.append(stats["mean"])
            curve.ci_lower.append(stats["ci_lower"])
            curve.ci_upper.append(stats["ci_upper"])

        result[metric_name] = curve

    return result
