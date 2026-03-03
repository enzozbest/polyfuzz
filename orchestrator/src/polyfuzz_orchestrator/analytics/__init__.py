"""Analytics and reporting for PolyFuzz experiments.

Public API:
    run_analytics(work_dir) -> AnalyticsResult
        Discovers campaign directories, collects metrics, computes aggregates,
        writes all output files, and prints a terminal summary.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from pathlib import Path

from polyfuzz_orchestrator.analytics.aggregator import (
    GrowthCurveData,
    compute_cross_campaign_stats,
    interpolate_growth_curves,
)
from polyfuzz_orchestrator.analytics.metrics import CampaignMetrics, collect_all_metrics
from polyfuzz_orchestrator.analytics.writers import print_terminal_summary, write_all_analytics

logger = logging.getLogger(__name__)


@dataclass
class AnalyticsResult:
    """Result of a complete analytics run.

    Attributes:
        metrics: Per-campaign metrics for all analyzed campaigns.
        stats: Cross-campaign aggregate statistics.
        curves: Growth curve data for each tracked metric.
        skipped: List of skipped campaign directory names.
        output_dir: Path to the analytics output directory.
    """

    metrics: list[CampaignMetrics] = field(default_factory=list)
    stats: dict[str, dict[str, float]] = field(default_factory=dict)
    curves: dict[str, GrowthCurveData] = field(default_factory=dict)
    skipped: list[str] = field(default_factory=list)
    output_dir: Path = field(default_factory=lambda: Path("."))


def run_analytics(work_dir: Path) -> AnalyticsResult:
    """Run the full analytics pipeline on an experiment directory.

    Discovers campaign_NNN directories, extracts metrics, computes cross-campaign statistics and growth curves,
    writes dual JSON/CSV output, and prints a Rich terminal summary.
    Args:
        work_dir: Path to the experiment working directory containing
                  campaign_NNN subdirectories.
    Returns:
        AnalyticsResult with all computed data and output directory path.
    Raises:
        ValueError: If no campaign directories are found in work_dir.
    """
    campaign_dirs = sorted(work_dir.glob("campaign_[0-9][0-9][0-9]"))
    if not campaign_dirs:
        msg = f"No campaign directories found in {work_dir}"
        raise ValueError(msg)

    metrics_internal, skipped = collect_all_metrics(campaign_dirs)
    analytics_dir = work_dir / "analytics"

    if not metrics_internal:
        logger.warning(
            "All %d campaigns were skipped -- no metrics to aggregate",
            len(campaign_dirs),
        )
        analytics_dir.mkdir(parents=True, exist_ok=True)
        from polyfuzz_orchestrator.analytics.writers import write_summary_json

        write_summary_json(analytics_dir, [], {}, skipped, work_dir)
        return AnalyticsResult(
            metrics=[],
            stats={},
            curves={},
            skipped=skipped,
            output_dir=analytics_dir,
        )

    stats = compute_cross_campaign_stats(metrics_internal)
    curves = interpolate_growth_curves(metrics_internal)

    analytics_dir.mkdir(parents=True, exist_ok=True)
    write_all_analytics(analytics_dir, metrics_internal, stats, curves, skipped, work_dir)

    print_terminal_summary(metrics_internal, stats, skipped)

    return AnalyticsResult(
        metrics=metrics_internal,
        stats=stats,
        curves=curves,
        skipped=skipped,
        output_dir=analytics_dir,
    )
