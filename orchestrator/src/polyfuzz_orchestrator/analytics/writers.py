"""Analytics output writers for dual JSON/CSV format and Rich terminal summary."""

from __future__ import annotations

import csv
import json
import os
import tempfile
from dataclasses import asdict
from pathlib import Path
from typing import TYPE_CHECKING

from rich.console import Console
from rich.table import Table

from polyfuzz_orchestrator.analytics.aggregator import GrowthCurveData
from polyfuzz_orchestrator.manifest import EXPERIMENT_MANIFEST_FILENAME, _atomic_write_json

if TYPE_CHECKING:
    from polyfuzz_orchestrator.analytics.metrics import CampaignMetrics

PRECISION: dict[str, str] = {
    "campaign_id": "",
    "seed": "d",
    "bitmap_cvg": ".4f",
    "edges_found": "d",
    "mismatch_count": "d",
    "mismatch_rate": ".4f",
    "corpus_initial": "d",
    "corpus_final": "d",
    "corpus_growth_pct": ".4f",
    "total_time_s": ".1f",
    "stage_smlgen_s": ".1f",
    "stage_afl_s": ".1f",
    "stage_diffcomp_s": ".1f",
}

MATRIX_COLUMNS: list[str] = [
    "campaign_id",
    "seed",
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
]

def _atomic_write_csv(path: Path, headers: list[str], rows: list[list[str]]) -> None:
    """Write a CSV file atomically using tempfile + os.replace.

    Args:
        path: Target file path.
        headers: Column header strings.
        rows: List of row data (each row is a list of formatted strings).
    """
    fd, tmp_path = tempfile.mkstemp(dir=path.parent, prefix=".csv_", suffix=".tmp")
    try:
        with os.fdopen(fd, "w", newline="") as f:
            writer = csv.writer(f, delimiter=",")
            writer.writerow(headers)
            writer.writerows(rows)
            f.flush()
            os.fsync(f.fileno())
        fd = -1  # Closed by os.fdopen context manager
        os.replace(tmp_path, path)
    except BaseException:
        if fd >= 0:
            os.close(fd)
        try:
            os.unlink(tmp_path)
        except OSError:
            pass
        raise


def _format_value(value: object, fmt: str) -> str:
    """Format a value using the given format specifier.

    Args:
        value: The value to format.
        fmt: Format specifier (e.g., ".4f", "d", "").
    Returns:
        Formatted string.
    """
    if fmt == "":
        return str(value)
    if fmt == "d":
        return f"{int(value):d}"
    return f"{value:{fmt}}"

def write_campaign_matrix(
    analytics_dir: Path, metrics: list[CampaignMetrics]
) -> None:
    """Write campaign matrix as JSON and CSV.

    JSON: list of dicts with all matrix columns.
    CSV: snake_case headers, precision-formatted values.
    Args:
        analytics_dir: Output directory for analytics files.
        metrics: List of CampaignMetrics, one per campaign.
    """

    # JSON output: list of dicts without plot_data
    json_rows = []
    for m in metrics:
        row = {col: getattr(m, col) for col in MATRIX_COLUMNS}
        json_rows.append(row)
    _atomic_write_json(analytics_dir / "campaign_matrix.json", json_rows)

    # CSV output: formatted with precision
    csv_rows = []
    for m in metrics:
        row = []
        for col in MATRIX_COLUMNS:
            val = getattr(m, col)
            fmt = PRECISION.get(col, "")
            row.append(_format_value(val, fmt))
        csv_rows.append(row)
    _atomic_write_csv(analytics_dir / "campaign_matrix.csv", MATRIX_COLUMNS, csv_rows)


def write_cross_campaign_summary(
    analytics_dir: Path, stats: dict[str, dict[str, float]]
) -> None:
    """Write cross-campaign summary statistics as JSON and CSV.

    JSON: metric_name -> {mean, median, stdev, ci_lower, ci_upper}.
    CSV: one row per metric, all stats to 4dp.
    Args:
        analytics_dir: Output directory for analytics files.
        stats: Cross-campaign statistics from compute_cross_campaign_stats.
    """
    _atomic_write_json(analytics_dir / "cross_campaign_summary.json", stats)

    headers = ["metric", "mean", "median", "stdev", "ci_lower", "ci_upper"]
    csv_rows = []
    for metric_name, metric_stats in stats.items():
        row = [
            metric_name,
            f"{metric_stats['mean']:.4f}",
            f"{metric_stats['median']:.4f}",
            f"{metric_stats['stdev']:.4f}",
            f"{metric_stats['ci_lower']:.4f}",
            f"{metric_stats['ci_upper']:.4f}",
        ]
        csv_rows.append(row)
    _atomic_write_csv(analytics_dir / "cross_campaign_summary.csv", headers, csv_rows)


def write_growth_curves(
    analytics_dir: Path, curves: dict[str, GrowthCurveData]
) -> None:
    """Write growth curves as JSON and wide-format CSV per metric.

    JSON: full GrowthCurveData structure.
    CSV: wide format with time_s, per-campaign columns, mean, ci_lower, ci_upper, campaign_count.
    Args:
        analytics_dir: Output directory for analytics files.
        curves: Growth curve data from interpolate_growth_curves.
    """
    for metric_name, curve in curves.items():
        # JSON output
        json_data = {
            "time_grid": curve.time_grid,
            "per_campaign": curve.per_campaign,
            "mean": curve.mean,
            "ci_lower": curve.ci_lower,
            "ci_upper": curve.ci_upper,
            "campaign_count": curve.campaign_count,
        }
        _atomic_write_json(
            analytics_dir / f"{metric_name}_over_time.json", json_data
        )

        # CSV output: wide format
        campaign_ids = sorted(curve.per_campaign.keys())
        headers = ["time_s"] + campaign_ids + ["mean", "ci_lower", "ci_upper", "campaign_count"]

        csv_rows = []
        for i, t in enumerate(curve.time_grid):
            row = [f"{t:.1f}"]
            for cid in campaign_ids:
                row.append(f"{curve.per_campaign[cid][i]:.4f}")
            row.append(f"{curve.mean[i]:.4f}")
            row.append(f"{curve.ci_lower[i]:.4f}")
            row.append(f"{curve.ci_upper[i]:.4f}")
            row.append(str(curve.campaign_count[i]))
            csv_rows.append(row)

        _atomic_write_csv(
            analytics_dir / f"{metric_name}_over_time.csv", headers, csv_rows
        )


def write_summary_json(
    analytics_dir: Path,
    metrics: list[CampaignMetrics],
    stats: dict[str, dict[str, float]],
    skipped: list[str],
    work_dir: Path,
) -> None:
    """Write experiment summary as JSON.

    Includes experiment metadata from the experiment manifest, key aggregate numbers, and list of skipped campaigns.
    Args:
        analytics_dir: Output directory for analytics files.
        metrics: List of CampaignMetrics for analyzed campaigns.
        stats: Cross-campaign statistics.
        skipped: List of skipped campaign names.
        work_dir: Experiment working directory (for reading experiment manifest).
    """
    manifest_path = work_dir / EXPERIMENT_MANIFEST_FILENAME
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        manifest = {}

    key_metrics = ["bitmap_cvg", "edges_found", "mismatch_rate"]
    key_aggregates = {k: stats[k] for k in key_metrics if k in stats}

    summary = {
        "experiment_metadata": {
            "master_seed": manifest.get("master_seed"),
            "num_campaigns": manifest.get("num_campaigns"),
            "campaigns_analyzed": len(metrics),
            "campaigns_skipped": len(skipped),
        },
        "key_aggregates": key_aggregates,
        "skipped_campaigns": skipped,
    }

    _atomic_write_json(analytics_dir / "summary.json", summary)


def print_terminal_summary(
    metrics: list[CampaignMetrics],
    stats: dict[str, dict[str, float]],
    skipped: list[str],
) -> None:
    """Print a Rich-formatted summary table to the terminal.
    Args:
        metrics: List of CampaignMetrics for analyzed campaigns.
        stats: Cross-campaign statistics.
        skipped: List of skipped campaign names.
    """
    console = Console()

    if not metrics:
        console.print("[yellow]No campaign metrics to display.[/yellow]")
        if skipped:
            console.print(
                f"[yellow]Skipped campaigns: {', '.join(skipped)}[/yellow]"
            )
        return

    table = Table(title="Campaign Results")
    table.add_column("Campaign", style="cyan")
    table.add_column("Coverage (%)", justify="right")
    table.add_column("Edges", justify="right")
    table.add_column("Mismatches", justify="right")
    table.add_column("Rate", justify="right")
    table.add_column("Corpus Growth (%)", justify="right")
    table.add_column("Time (s)", justify="right")

    for m in metrics:
        table.add_row(
            m.campaign_id,
            f"{m.bitmap_cvg:.2f}",
            str(m.edges_found),
            str(m.mismatch_count),
            f"{m.mismatch_rate:.4f}",
            f"{m.corpus_growth_pct:.1f}",
            f"{m.total_time_s:.1f}",
        )

    console.print(table)

    if stats:
        cvg = stats.get("bitmap_cvg", {})
        edges = stats.get("edges_found", {})
        rate = stats.get("mismatch_rate", {})

        console.print(
            f"\n[bold]Aggregates:[/bold] "
            f"Coverage {cvg.get('mean', 0):.2f}% "
            f"(CI: {cvg.get('ci_lower', 0):.2f}-{cvg.get('ci_upper', 0):.2f}), "
            f"Edges {edges.get('mean', 0):.0f} "
            f"(CI: {edges.get('ci_lower', 0):.0f}-{edges.get('ci_upper', 0):.0f}), "
            f"Mismatch rate {rate.get('mean', 0):.4f} "
            f"(CI: {rate.get('ci_lower', 0):.4f}-{rate.get('ci_upper', 0):.4f})"
        )

    if skipped:
        console.print(
            f"\n[yellow]Warning: {len(skipped)} campaign(s) skipped: "
            f"{', '.join(skipped)}[/yellow]"
        )


def write_all_analytics(
    analytics_dir: Path,
    metrics: list[CampaignMetrics],
    stats: dict[str, dict[str, float]],
    curves: dict[str, GrowthCurveData],
    skipped: list[str],
    work_dir: Path,
) -> None:
    """Write all analytics output files.

    Convenience function that calls all individual writer functions.
    Args:
        analytics_dir: Output directory for analytics files.
        metrics: List of CampaignMetrics for analyzed campaigns.
        stats: Cross-campaign statistics.
        curves: Growth curve data.
        skipped: List of skipped campaign names.
        work_dir: Experiment working directory.
    """
    analytics_dir.mkdir(parents=True, exist_ok=True)
    write_campaign_matrix(analytics_dir, metrics)
    write_cross_campaign_summary(analytics_dir, stats)
    write_growth_curves(analytics_dir, curves)
    write_summary_json(analytics_dir, metrics, stats, skipped, work_dir)
