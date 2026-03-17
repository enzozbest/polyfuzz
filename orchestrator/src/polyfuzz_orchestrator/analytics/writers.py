"""Analytics output writers for dual JSON/CSV format and Rich terminal summary."""

from __future__ import annotations

import csv
import json
import os
import tempfile
from pathlib import Path
from typing import TYPE_CHECKING

from rich.console import Console
from rich.table import Table

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
    "stage_coverage_s": ".1f",
    "branch_total": "d",
    "branch_covered": "d",
    "branch_coverage_pct": ".2f",
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
    "stage_coverage_s",
    "branch_total",
    "branch_covered",
    "branch_coverage_pct",
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

    # JSON output: list of dicts
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


def write_summary_json(
    analytics_dir: Path,
    metrics: list[CampaignMetrics],
    skipped: list[str],
    work_dir: Path,
) -> None:
    """Write experiment summary as JSON.

    Includes experiment metadata from the experiment manifest and list of skipped campaigns.
    Args:
        analytics_dir: Output directory for analytics files.
        metrics: List of CampaignMetrics for analyzed campaigns.
        skipped: List of skipped campaign names.
        work_dir: Experiment working directory (for reading experiment manifest).
    """
    manifest_path = work_dir / EXPERIMENT_MANIFEST_FILENAME
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        manifest = {}

    summary = {
        "experiment_metadata": {
            "master_seed": manifest.get("master_seed"),
            "num_campaigns": manifest.get("num_campaigns"),
            "campaigns_analyzed": len(metrics),
            "campaigns_skipped": len(skipped),
        },
        "skipped_campaigns": skipped,
    }

    _atomic_write_json(analytics_dir / "summary.json", summary)


def print_terminal_summary(
    metrics: list[CampaignMetrics],
    skipped: list[str],
) -> None:
    """Print a Rich-formatted summary table to the terminal.
    Args:
        metrics: List of CampaignMetrics for analyzed campaigns.
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
    table.add_column("Branch Cvg (%)", justify="right")
    table.add_column("Time (s)", justify="right")

    for m in metrics:
        table.add_row(
            m.campaign_id,
            f"{m.bitmap_cvg:.2f}",
            str(m.edges_found),
            str(m.mismatch_count),
            f"{m.mismatch_rate:.4f}",
            f"{m.corpus_growth_pct:.1f}",
            f"{m.branch_coverage_pct:.2f}",
            f"{m.total_time_s:.1f}",
        )

    console.print(table)

    if skipped:
        console.print(
            f"\n[yellow]Warning: {len(skipped)} campaign(s) skipped: "
            f"{', '.join(skipped)}[/yellow]"
        )


def write_all_analytics(
    analytics_dir: Path,
    metrics: list[CampaignMetrics],
    skipped: list[str],
    work_dir: Path,
) -> None:
    """Write all analytics output files.

    Convenience function that calls all individual writer functions.
    Args:
        analytics_dir: Output directory for analytics files.
        metrics: List of CampaignMetrics for analyzed campaigns.
        skipped: List of skipped campaign names.
        work_dir: Experiment working directory.
    """
    analytics_dir.mkdir(parents=True, exist_ok=True)
    write_campaign_matrix(analytics_dir, metrics)
    write_summary_json(analytics_dir, metrics, skipped, work_dir)
