"""Tests for analytics writers module and run_analytics public API.

Tests cover dual JSON/CSV output for campaign matrix, cross-campaign summary,
growth curves, summary.json, Rich terminal summary, and the full run_analytics
pipeline.
"""

from __future__ import annotations

import csv
import json
from pathlib import Path
from unittest.mock import patch

import pytest

from polyfuzz_orchestrator.analytics import AnalyticsResult, run_analytics
from polyfuzz_orchestrator.analytics.aggregator import (
    GrowthCurveData,
    compute_cross_campaign_stats,
    interpolate_growth_curves,
)
from polyfuzz_orchestrator.analytics.metrics import CampaignMetrics
from polyfuzz_orchestrator.analytics.writers import (
    print_terminal_summary,
    write_all_analytics,
    write_campaign_matrix,
    write_cross_campaign_summary,
    write_growth_curves,
    write_summary_json,
)


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


def _make_metrics(
    campaign_id: str = "campaign_000",
    seed: int = 42,
    bitmap_cvg: float = 10.5,
    edges_found: int = 100,
    mismatch_count: int = 5,
    mismatch_rate: float = 0.05,
    corpus_initial: int = 10,
    corpus_final: int = 20,
    corpus_growth_pct: float = 100.0,
    total_time_s: float = 60.0,
    stage_smlgen_s: float = 5.0,
    stage_afl_s: float = 50.0,
    stage_diffcomp_s: float = 5.0,
    plot_data: list[dict[str, float]] | None = None,
) -> CampaignMetrics:
    if plot_data is None:
        plot_data = [
            {"relative_time": 0.0, "edges_found": 0.0, "map_size": 0.0,
             "total_execs": 0.0, "total_crashes": 0.0, "corpus_count": 10.0},
            {"relative_time": 30.0, "edges_found": 50.0, "map_size": 5.0,
             "total_execs": 1000.0, "total_crashes": 0.0, "corpus_count": 15.0},
            {"relative_time": 60.0, "edges_found": 100.0, "map_size": 10.0,
             "total_execs": 2000.0, "total_crashes": 1.0, "corpus_count": 20.0},
        ]
    return CampaignMetrics(
        campaign_id=campaign_id,
        seed=seed,
        bitmap_cvg=bitmap_cvg,
        edges_found=edges_found,
        mismatch_count=mismatch_count,
        mismatch_rate=mismatch_rate,
        corpus_initial=corpus_initial,
        corpus_final=corpus_final,
        corpus_growth_pct=corpus_growth_pct,
        total_time_s=total_time_s,
        stage_smlgen_s=stage_smlgen_s,
        stage_afl_s=stage_afl_s,
        stage_diffcomp_s=stage_diffcomp_s,
        plot_data=plot_data,
    )


def _make_two_metrics() -> list[CampaignMetrics]:
    return [
        _make_metrics(campaign_id="campaign_000", seed=42, bitmap_cvg=10.0, edges_found=100),
        _make_metrics(campaign_id="campaign_001", seed=99, bitmap_cvg=20.0, edges_found=200),
    ]


@pytest.fixture
def analytics_dir(tmp_path: Path) -> Path:
    d = tmp_path / "analytics"
    d.mkdir()
    return d


@pytest.fixture
def two_metrics() -> list[CampaignMetrics]:
    return _make_two_metrics()


@pytest.fixture
def two_stats(two_metrics: list[CampaignMetrics]) -> dict[str, dict[str, float]]:
    return compute_cross_campaign_stats(two_metrics)


@pytest.fixture
def two_curves(two_metrics: list[CampaignMetrics]) -> dict[str, GrowthCurveData]:
    return interpolate_growth_curves(two_metrics)


# ---------------------------------------------------------------------------
# write_campaign_matrix
# ---------------------------------------------------------------------------


class TestWriteCampaignMatrix:
    """Tests for write_campaign_matrix function."""

    def test_creates_json_and_csv_files(
        self, analytics_dir: Path, two_metrics: list[CampaignMetrics]
    ) -> None:
        write_campaign_matrix(analytics_dir, two_metrics)
        assert (analytics_dir / "campaign_matrix.json").exists()
        assert (analytics_dir / "campaign_matrix.csv").exists()

    def test_json_has_correct_keys(
        self, analytics_dir: Path, two_metrics: list[CampaignMetrics]
    ) -> None:
        write_campaign_matrix(analytics_dir, two_metrics)
        data = json.loads((analytics_dir / "campaign_matrix.json").read_text())
        assert len(data) == 2
        row = data[0]
        assert "campaign_id" in row
        assert "seed" in row
        assert "bitmap_cvg" in row
        assert "edges_found" in row
        assert "mismatch_count" in row
        assert "mismatch_rate" in row
        assert "corpus_initial" in row
        assert "corpus_final" in row
        assert "corpus_growth_pct" in row
        assert "total_time_s" in row
        assert "stage_smlgen_s" in row
        assert "stage_afl_s" in row
        assert "stage_diffcomp_s" in row
        # plot_data should NOT be in matrix
        assert "plot_data" not in row

    def test_csv_has_snake_case_headers(
        self, analytics_dir: Path, two_metrics: list[CampaignMetrics]
    ) -> None:
        write_campaign_matrix(analytics_dir, two_metrics)
        with open(analytics_dir / "campaign_matrix.csv") as f:
            reader = csv.reader(f)
            headers = next(reader)
        assert headers[0] == "campaign_id"
        # All headers should be snake_case
        for h in headers:
            assert h == h.lower()
            assert " " not in h

    def test_csv_precision_integers_no_decimals(
        self, analytics_dir: Path
    ) -> None:
        m = _make_metrics(edges_found=150, corpus_initial=10, corpus_final=25, mismatch_count=3)
        write_campaign_matrix(analytics_dir, [m])
        with open(analytics_dir / "campaign_matrix.csv") as f:
            reader = csv.reader(f)
            headers = next(reader)
            row = next(reader)
        row_dict = dict(zip(headers, row))
        # Integers should not have decimal points
        assert row_dict["edges_found"] == "150"
        assert row_dict["corpus_initial"] == "10"
        assert row_dict["corpus_final"] == "25"
        assert row_dict["mismatch_count"] == "3"

    def test_csv_precision_rates_4dp(
        self, analytics_dir: Path
    ) -> None:
        m = _make_metrics(mismatch_rate=0.123456789, bitmap_cvg=45.123456789)
        write_campaign_matrix(analytics_dir, [m])
        with open(analytics_dir / "campaign_matrix.csv") as f:
            reader = csv.reader(f)
            headers = next(reader)
            row = next(reader)
        row_dict = dict(zip(headers, row))
        assert row_dict["mismatch_rate"] == "0.1235"
        assert row_dict["bitmap_cvg"] == "45.1235"

    def test_csv_precision_times_1dp(
        self, analytics_dir: Path
    ) -> None:
        m = _make_metrics(total_time_s=123.456, stage_afl_s=99.999)
        write_campaign_matrix(analytics_dir, [m])
        with open(analytics_dir / "campaign_matrix.csv") as f:
            reader = csv.reader(f)
            headers = next(reader)
            row = next(reader)
        row_dict = dict(zip(headers, row))
        assert row_dict["total_time_s"] == "123.5"
        assert row_dict["stage_afl_s"] == "100.0"


# ---------------------------------------------------------------------------
# write_cross_campaign_summary
# ---------------------------------------------------------------------------


class TestWriteCrossCampaignSummary:
    """Tests for write_cross_campaign_summary function."""

    def test_creates_json_and_csv_files(
        self, analytics_dir: Path, two_stats: dict
    ) -> None:
        write_cross_campaign_summary(analytics_dir, two_stats)
        assert (analytics_dir / "cross_campaign_summary.json").exists()
        assert (analytics_dir / "cross_campaign_summary.csv").exists()

    def test_json_structure(
        self, analytics_dir: Path, two_stats: dict
    ) -> None:
        write_cross_campaign_summary(analytics_dir, two_stats)
        data = json.loads((analytics_dir / "cross_campaign_summary.json").read_text())
        assert "bitmap_cvg" in data
        assert "mean" in data["bitmap_cvg"]
        assert "median" in data["bitmap_cvg"]
        assert "stdev" in data["bitmap_cvg"]
        assert "ci_lower" in data["bitmap_cvg"]
        assert "ci_upper" in data["bitmap_cvg"]

    def test_csv_has_metric_rows(
        self, analytics_dir: Path, two_stats: dict
    ) -> None:
        write_cross_campaign_summary(analytics_dir, two_stats)
        with open(analytics_dir / "cross_campaign_summary.csv") as f:
            reader = csv.reader(f)
            headers = next(reader)
            rows = list(reader)
        assert headers == ["metric", "mean", "median", "stdev", "ci_lower", "ci_upper"]
        metric_names = [r[0] for r in rows]
        assert "bitmap_cvg" in metric_names
        assert "edges_found" in metric_names


# ---------------------------------------------------------------------------
# write_growth_curves
# ---------------------------------------------------------------------------


class TestWriteGrowthCurves:
    """Tests for write_growth_curves function."""

    def test_creates_files_per_metric(
        self, analytics_dir: Path, two_curves: dict
    ) -> None:
        write_growth_curves(analytics_dir, two_curves)
        for metric_name in two_curves:
            assert (analytics_dir / f"{metric_name}_over_time.json").exists()
            assert (analytics_dir / f"{metric_name}_over_time.csv").exists()

    def test_csv_wide_format_headers(
        self, analytics_dir: Path, two_curves: dict
    ) -> None:
        write_growth_curves(analytics_dir, two_curves)
        # Check edges_found
        with open(analytics_dir / "edges_found_over_time.csv") as f:
            reader = csv.reader(f)
            headers = next(reader)
        assert headers[0] == "time_s"
        # Should have per-campaign columns
        assert "campaign_000" in headers
        assert "campaign_001" in headers
        # Should end with aggregate columns
        assert "mean" in headers
        assert "ci_lower" in headers
        assert "ci_upper" in headers
        assert "campaign_count" in headers

    def test_json_has_expected_keys(
        self, analytics_dir: Path, two_curves: dict
    ) -> None:
        write_growth_curves(analytics_dir, two_curves)
        data = json.loads((analytics_dir / "edges_found_over_time.json").read_text())
        assert "time_grid" in data
        assert "per_campaign" in data
        assert "mean" in data
        assert "ci_lower" in data
        assert "ci_upper" in data
        assert "campaign_count" in data


# ---------------------------------------------------------------------------
# write_summary_json
# ---------------------------------------------------------------------------


class TestWriteSummaryJson:
    """Tests for write_summary_json function."""

    def test_creates_summary_json(
        self, analytics_dir: Path, two_metrics: list[CampaignMetrics], two_stats: dict, tmp_path: Path
    ) -> None:
        # Create experiment manifest
        work_dir = tmp_path / "experiment"
        work_dir.mkdir()
        manifest = {
            "master_seed": 12345,
            "num_campaigns": 5,
            "config": {},
            "started_at": "2026-01-01T00:00:00Z",
            "campaigns": [],
        }
        (work_dir / "experiment_manifest.json").write_text(json.dumps(manifest))

        write_summary_json(analytics_dir, two_metrics, two_stats, ["campaign_002"], work_dir)

        summary = json.loads((analytics_dir / "summary.json").read_text())
        assert summary["experiment_metadata"]["master_seed"] == 12345
        assert summary["experiment_metadata"]["num_campaigns"] == 5
        assert summary["experiment_metadata"]["campaigns_analyzed"] == 2
        assert summary["experiment_metadata"]["campaigns_skipped"] == 1
        assert "key_aggregates" in summary
        assert "bitmap_cvg" in summary["key_aggregates"]
        assert summary["skipped_campaigns"] == ["campaign_002"]


# ---------------------------------------------------------------------------
# print_terminal_summary
# ---------------------------------------------------------------------------


class TestPrintTerminalSummary:
    """Tests for print_terminal_summary function."""

    def test_no_exception(
        self, two_metrics: list[CampaignMetrics], two_stats: dict
    ) -> None:
        """Should not raise any exceptions."""
        print_terminal_summary(two_metrics, two_stats, [])

    def test_with_skipped_campaigns(
        self, two_metrics: list[CampaignMetrics], two_stats: dict
    ) -> None:
        """Should handle skipped campaigns without error."""
        print_terminal_summary(two_metrics, two_stats, ["campaign_002", "campaign_003"])

    def test_empty_metrics(self) -> None:
        """Should handle empty metrics list."""
        print_terminal_summary([], {}, [])


# ---------------------------------------------------------------------------
# write_all_analytics
# ---------------------------------------------------------------------------


class TestWriteAllAnalytics:
    """Tests for write_all_analytics convenience function."""

    def test_creates_all_output_files(
        self,
        tmp_path: Path,
        two_metrics: list[CampaignMetrics],
        two_stats: dict,
        two_curves: dict,
    ) -> None:
        work_dir = tmp_path / "experiment"
        work_dir.mkdir()
        manifest = {
            "master_seed": 12345,
            "num_campaigns": 5,
            "config": {},
            "started_at": "2026-01-01T00:00:00Z",
            "campaigns": [],
        }
        (work_dir / "experiment_manifest.json").write_text(json.dumps(manifest))

        analytics_dir = tmp_path / "analytics"
        # write_all_analytics should create the dir
        write_all_analytics(analytics_dir, two_metrics, two_stats, two_curves, [], work_dir)

        assert analytics_dir.exists()
        assert (analytics_dir / "campaign_matrix.json").exists()
        assert (analytics_dir / "campaign_matrix.csv").exists()
        assert (analytics_dir / "cross_campaign_summary.json").exists()
        assert (analytics_dir / "cross_campaign_summary.csv").exists()
        assert (analytics_dir / "summary.json").exists()
        # Growth curve files
        for metric_name in two_curves:
            assert (analytics_dir / f"{metric_name}_over_time.json").exists()
            assert (analytics_dir / f"{metric_name}_over_time.csv").exists()


# ---------------------------------------------------------------------------
# run_analytics (integration test)
# ---------------------------------------------------------------------------


def _setup_mock_campaign(
    work_dir: Path,
    campaign_id: str,
    seed: int,
    duration: float = 60.0,
) -> Path:
    """Create a minimal mock campaign directory for integration testing."""
    campaign_dir = work_dir / campaign_id
    campaign_dir.mkdir(parents=True)

    # Campaign manifest
    manifest = {
        "status": "complete",
        "campaign_seed": seed,
        "timing": {
            "duration_seconds": duration,
            "stages": {"smlgen": 5.0, "afl": 50.0, "diffcomp": 5.0},
        },
    }
    (campaign_dir / "manifest.json").write_text(json.dumps(manifest))

    # AFL++ output directories
    afl_output = campaign_dir / "afl_output" / "default"
    queue_dir = afl_output / "queue"
    queue_dir.mkdir(parents=True)
    for i in range(5):
        (queue_dir / f"id:{i:06d},src:000000").write_text(f"test{i}")

    # fuzzer_stats
    (afl_output / "fuzzer_stats").write_text(
        f"bitmap_cvg        : {10.0 + seed % 20:.2f}%\n"
        f"edges_found       : {100 + seed}\n"
    )

    # plot_data
    (afl_output / "plot_data").write_text(
        "# relative_time, cycles_done, cur_item, corpus_count, pending_total, "
        "pending_favs, map_size, saved_crashes, saved_hangs, max_depth, "
        "execs_per_sec, total_execs, edges_found, total_crashes, servers_count\n"
        f"0, 0, 0, 5, 0, 0, 0.0, 0, 0, 0, 0.0, 0, 0, 0, 0\n"
        f"30, 1, 2, 8, 1, 0, 5.0, 0, 0, 1, 100.0, 500, 50, 0, 0\n"
        f"60, 2, 4, 12, 0, 0, 10.0, 0, 0, 2, 150.0, 1000, {100 + seed}, 0, 0\n"
    )

    # Corpus directory
    corpus_dir = campaign_dir / "corpus"
    corpus_dir.mkdir()
    for i in range(3):
        (corpus_dir / f"seed_{i}.sml").write_text(f"seed{i}")

    # Diffcomp output
    diffcomp_dir = campaign_dir / "diffcomp_output"
    diffcomp_dir.mkdir()
    for i in range(4):
        status = "MATCH" if i < 3 else "DIFF"
        (diffcomp_dir / f"report_{i}.json").write_text(json.dumps({"status": status}))

    return campaign_dir


class TestRunAnalytics:
    """Integration tests for run_analytics public API."""

    def test_produces_all_output_files(self, tmp_path: Path) -> None:
        work_dir = tmp_path / "experiment"
        work_dir.mkdir()

        # Create experiment manifest
        manifest = {
            "master_seed": 12345,
            "num_campaigns": 2,
            "config": {},
            "started_at": "2026-01-01T00:00:00Z",
            "campaigns": [],
        }
        (work_dir / "experiment_manifest.json").write_text(json.dumps(manifest))

        _setup_mock_campaign(work_dir, "campaign_000", seed=42)
        _setup_mock_campaign(work_dir, "campaign_001", seed=99)

        result = run_analytics(work_dir)

        assert isinstance(result, AnalyticsResult)
        assert len(result.metrics) == 2
        assert result.output_dir == work_dir / "analytics"
        assert result.output_dir.exists()

        # Check all output files
        ad = result.output_dir
        assert (ad / "campaign_matrix.json").exists()
        assert (ad / "campaign_matrix.csv").exists()
        assert (ad / "cross_campaign_summary.json").exists()
        assert (ad / "cross_campaign_summary.csv").exists()
        assert (ad / "summary.json").exists()

    def test_returns_analytics_result_with_stats(self, tmp_path: Path) -> None:
        work_dir = tmp_path / "experiment"
        work_dir.mkdir()

        manifest = {
            "master_seed": 12345,
            "num_campaigns": 2,
            "config": {},
            "started_at": "2026-01-01T00:00:00Z",
            "campaigns": [],
        }
        (work_dir / "experiment_manifest.json").write_text(json.dumps(manifest))

        _setup_mock_campaign(work_dir, "campaign_000", seed=42)
        _setup_mock_campaign(work_dir, "campaign_001", seed=99)

        result = run_analytics(work_dir)

        assert "bitmap_cvg" in result.stats
        assert "mean" in result.stats["bitmap_cvg"]
        assert len(result.curves) > 0
        assert result.skipped == []

    def test_no_campaign_dirs_raises(self, tmp_path: Path) -> None:
        work_dir = tmp_path / "experiment"
        work_dir.mkdir()
        with pytest.raises(ValueError, match="No campaign directories"):
            run_analytics(work_dir)

    def test_all_campaigns_skipped_warns(self, tmp_path: Path) -> None:
        """When all campaigns are incomplete, should warn and return empty result."""
        work_dir = tmp_path / "experiment"
        work_dir.mkdir()

        manifest = {
            "master_seed": 12345,
            "num_campaigns": 1,
            "config": {},
            "started_at": "2026-01-01T00:00:00Z",
            "campaigns": [],
        }
        (work_dir / "experiment_manifest.json").write_text(json.dumps(manifest))

        # Create incomplete campaign (no manifest.json with status=complete)
        campaign_dir = work_dir / "campaign_000"
        campaign_dir.mkdir()
        (campaign_dir / "manifest.json").write_text(json.dumps({"status": "running"}))

        result = run_analytics(work_dir)
        assert len(result.metrics) == 0
        assert result.skipped == ["campaign_000"]
        assert result.output_dir == work_dir / "analytics"

    def test_creates_analytics_directory(self, tmp_path: Path) -> None:
        work_dir = tmp_path / "experiment"
        work_dir.mkdir()

        manifest = {
            "master_seed": 12345,
            "num_campaigns": 1,
            "config": {},
            "started_at": "2026-01-01T00:00:00Z",
            "campaigns": [],
        }
        (work_dir / "experiment_manifest.json").write_text(json.dumps(manifest))

        _setup_mock_campaign(work_dir, "campaign_000", seed=42)

        assert not (work_dir / "analytics").exists()
        result = run_analytics(work_dir)
        assert result.output_dir.exists()
