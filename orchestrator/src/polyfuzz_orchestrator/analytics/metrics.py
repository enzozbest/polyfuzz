"""Per-campaign metric extraction and CampaignMetrics dataclass.

Composes parsers from analytics.parsers to produce structured metrics
for each completed campaign. Handles missing data and edge cases gracefully.
"""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass
from pathlib import Path

from polyfuzz_orchestrator.analytics.parsers import (
    parse_coverage_summary,
    parse_diffcomp_reports,
    parse_fuzzer_stats,
)
from polyfuzz_orchestrator.manifest import is_campaign_complete
from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class CampaignMetrics:
    """Metrics extracted from a single completed campaign.

    All numeric fields are pre-computed from raw campaign output files. The dataclass is frozen (immutable)
    for safety during aggregation.
    """

    campaign_id: str
    seed: int
    bitmap_cvg: float
    edges_found: int
    mismatch_count: int
    mismatch_rate: float
    corpus_initial: int
    corpus_final: int
    corpus_growth_pct: float
    total_time_s: float
    stage_smlgen_s: float
    stage_afl_s: float
    stage_diffcomp_s: float
    stage_coverage_s: float
    branch_total: int
    branch_covered: int
    branch_coverage_pct: float


def _count_files(directory: Path) -> int:
    """Count files in a directory, excluding dotfiles and README.txt.

    Matches the filter pattern from DiffcompStage._list_input_files.
    Args:
        directory: Path to the directory to count files in.
    Returns:
        Number of qualifying files, or 0 if directory doesn't exist.
    """
    try:
        return len([
            f
            for f in directory.iterdir()
            if f.is_file() and not f.name.startswith(".") and f.name != "README.txt"
        ])
    except (FileNotFoundError, OSError):
        return 0


def extract_campaign_metrics(campaign_dir: Path) -> CampaignMetrics | None:
    """Extract metrics from a single completed campaign directory.

    Reads manifest.json for timing and seed, calls parsers for AFL++ stats and diffcomp reports, counts corpus files,
    and computes derived metrics.
    Args:
        campaign_dir: Path to a campaign directory (e.g., campaign_000/).
    Returns:
        CampaignMetrics if campaign is complete, None otherwise.
    """

    if not is_campaign_complete(campaign_dir):
        logger.debug("Skipping incomplete campaign: %s", campaign_dir.name)
        return None

    try:
        manifest = json.loads(
            (campaign_dir / "manifest.json").read_text(encoding="utf-8")
        )
    except (json.JSONDecodeError, OSError) as exc:
        logger.warning("Cannot read manifest for %s: %s", campaign_dir.name, exc)
        return None

    timing = manifest.get("timing", {})
    stages = timing.get("stages", {})
    campaign_seed = manifest.get("campaign_seed", 0)

    # Locate AFL++ fuzzer output directory (handles both single and parallel mode)
    queue_dir = DiffcompStage._find_queue_dir(campaign_dir)
    if queue_dir is not None:
        fuzzer_dir = queue_dir.parent
    else:
        # Fall back to conventional path for metrics even if queue is missing
        fuzzer_dir = campaign_dir / "afl_output" / "default"

    fuzzer_stats = parse_fuzzer_stats(fuzzer_dir / "fuzzer_stats")
    bitmap_cvg = float(fuzzer_stats.get("bitmap_cvg", 0.0))
    edges_found = int(fuzzer_stats.get("edges_found", 0))

    match_count, diff_count, failure_count = parse_diffcomp_reports(
        campaign_dir / "diffcomp_output"
    )
    mismatch_count = diff_count
    total_compared = match_count + diff_count
    mismatch_rate = diff_count / total_compared if total_compared > 0 else 0.0

    corpus_initial = _count_files(campaign_dir / "corpus")
    corpus_final = _count_files(queue_dir) if queue_dir is not None else 0
    corpus_growth_pct = (
        ((corpus_final - corpus_initial) / corpus_initial * 100.0)
        if corpus_initial > 0
        else 0.0
    )

    coverage = parse_coverage_summary(
        campaign_dir / "coverage_out" / "coverage_summary.json"
    )

    return CampaignMetrics(
        campaign_id=campaign_dir.name,
        seed=campaign_seed,
        bitmap_cvg=bitmap_cvg,
        edges_found=edges_found,
        mismatch_count=mismatch_count,
        mismatch_rate=mismatch_rate,
        corpus_initial=corpus_initial,
        corpus_final=corpus_final,
        corpus_growth_pct=corpus_growth_pct,
        total_time_s=timing.get("duration_seconds", 0.0),
        stage_smlgen_s=stages.get("smlgen", 0.0),
        stage_afl_s=stages.get("afl", 0.0),
        stage_diffcomp_s=stages.get("diffcomp", 0.0),
        stage_coverage_s=stages.get("coverage", 0.0),
        branch_total=int(coverage.get("total_branches", 0)),
        branch_covered=int(coverage.get("covered_branches", 0)),
        branch_coverage_pct=float(coverage.get("branch_coverage_pct", 0.0)),
    )


def collect_all_metrics(
    campaign_dirs: list[Path],
) -> tuple[list[CampaignMetrics], list[str]]:
    """Extract metrics from multiple campaign directories.

    Iterates campaign directories, extracts metrics for complete campaigns, and reports which campaigns were skipped.
    Args:
        campaign_dirs: List of campaign directory paths.
    Returns:
        Tuple of (list of CampaignMetrics for complete campaigns,
                  list of skipped campaign directory names).
    """
    metrics: list[CampaignMetrics] = []
    skipped: list[str] = []

    for campaign_dir in campaign_dirs:
        result = extract_campaign_metrics(campaign_dir)
        if result is not None:
            metrics.append(result)
        else:
            skipped.append(campaign_dir.name)

    return metrics, skipped
