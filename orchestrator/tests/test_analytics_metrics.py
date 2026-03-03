"""Tests for analytics metrics: CampaignMetrics and extraction functions."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from polyfuzz_orchestrator.analytics.metrics import (
    CampaignMetrics,
    collect_all_metrics,
    extract_campaign_metrics,
)


def _create_complete_campaign(
    campaign_dir: Path,
    *,
    campaign_seed: int = 67890,
    duration_seconds: float = 330.0,
    stage_smlgen: float = 5.2,
    stage_afl: float = 300.1,
    stage_diffcomp: float = 24.7,
    bitmap_cvg: float = 45.23,
    edges_found: int = 1234,
    corpus_count: int = 567,
    initial_corpus_files: int = 100,
    queue_files: int = 150,
    match_count: int = 140,
    diff_count: int = 8,
    failure_count: int = 2,
) -> None:
    """Create a realistic complete campaign directory structure for testing."""
    campaign_dir.mkdir(parents=True, exist_ok=True)

    # Write manifest.json
    manifest = {
        "status": "complete",
        "campaign_index": 0,
        "master_seed": 12345,
        "campaign_seed": campaign_seed,
        "config": {
            "tests_per_campaign": 100,
            "afl_timeout_s": 300,
            "stage_timeout_s": 600,
        },
        "timing": {
            "start": "2026-03-03T10:00:00+00:00",
            "end": "2026-03-03T10:05:30+00:00",
            "duration_seconds": duration_seconds,
            "stages": {
                "smlgen": stage_smlgen,
                "afl": stage_afl,
                "diffcomp": stage_diffcomp,
            },
        },
        "metadata": {},
    }
    (campaign_dir / "manifest.json").write_text(json.dumps(manifest))

    # Write fuzzer_stats
    afl_default = campaign_dir / "afl_output" / "default"
    afl_default.mkdir(parents=True)
    (afl_default / "fuzzer_stats").write_text(
        f"bitmap_cvg        : {bitmap_cvg}%\n"
        f"edges_found       : {edges_found}\n"
        f"corpus_count      : {corpus_count}\n"
        f"execs_per_sec     : 1200.50\n"
    )

    # Write plot_data
    (afl_default / "plot_data").write_text(
        "# relative_time, cycles_done, cur_item, corpus_count, "
        "pending_total, pending_favs, map_size, saved_crashes, "
        "saved_hangs, max_depth, execs_per_sec, total_execs, "
        "edges_found, total_crashes, servers_count\n"
        "0, 0, 0, 100, 100, 50, 0.05, 0, 0, 1, 500.00, 0, 50, 0, 0\n"
        "5, 0, 15, 105, 85, 45, 1.23, 0, 0, 2, 1200.50, 6002, 120, 0, 0\n"
    )

    # Create queue directory with files
    queue_dir = afl_default / "queue"
    queue_dir.mkdir()
    for i in range(queue_files):
        (queue_dir / f"id:{i:06d},time:0,execs:0,orig:test").write_text(f"test {i}")
    # Add dotfile and README.txt that should be excluded
    (queue_dir / ".state").write_text("")
    (queue_dir / "README.txt").write_text("AFL++ readme")

    # Create corpus directory
    corpus_dir = campaign_dir / "corpus"
    corpus_dir.mkdir()
    for i in range(initial_corpus_files):
        (corpus_dir / f"input_{i:04d}.sml").write_text(f"corpus {i}")

    # Create diffcomp_output with JSON reports
    diffcomp_dir = campaign_dir / "diffcomp_output"
    diffcomp_dir.mkdir()
    file_idx = 0
    for _ in range(match_count):
        (diffcomp_dir / f"file_{file_idx:04d}.json").write_text(
            json.dumps({"status": "MATCH", "mismatchCount": 0})
        )
        file_idx += 1
    for _ in range(diff_count):
        (diffcomp_dir / f"file_{file_idx:04d}.json").write_text(
            json.dumps({"status": "DIFF", "mismatchCount": 2})
        )
        file_idx += 1
    for _ in range(failure_count):
        (diffcomp_dir / f"file_{file_idx:04d}.json").write_text(
            json.dumps({"status": "FAILURE", "error": "lexer error"})
        )
        file_idx += 1


# ---------------------------------------------------------------------------
# CampaignMetrics dataclass tests
# ---------------------------------------------------------------------------


class TestCampaignMetrics:
    """Tests for CampaignMetrics dataclass."""

    def test_has_all_required_fields(self) -> None:
        metrics = CampaignMetrics(
            campaign_id="campaign_000",
            seed=67890,
            bitmap_cvg=45.23,
            edges_found=1234,
            mismatch_count=8,
            mismatch_rate=0.0541,
            corpus_initial=100,
            corpus_final=150,
            corpus_growth_pct=50.0,
            total_time_s=330.0,
            stage_smlgen_s=5.2,
            stage_afl_s=300.1,
            stage_diffcomp_s=24.7,
            plot_data=[],
        )
        assert metrics.campaign_id == "campaign_000"
        assert metrics.seed == 67890
        assert metrics.bitmap_cvg == pytest.approx(45.23)
        assert metrics.edges_found == 1234
        assert metrics.mismatch_count == 8
        assert metrics.mismatch_rate == pytest.approx(0.0541)
        assert metrics.corpus_initial == 100
        assert metrics.corpus_final == 150
        assert metrics.corpus_growth_pct == pytest.approx(50.0)
        assert metrics.total_time_s == pytest.approx(330.0)
        assert metrics.stage_smlgen_s == pytest.approx(5.2)
        assert metrics.stage_afl_s == pytest.approx(300.1)
        assert metrics.stage_diffcomp_s == pytest.approx(24.7)
        assert metrics.plot_data == []

    def test_is_frozen(self) -> None:
        metrics = CampaignMetrics(
            campaign_id="campaign_000",
            seed=0,
            bitmap_cvg=0.0,
            edges_found=0,
            mismatch_count=0,
            mismatch_rate=0.0,
            corpus_initial=0,
            corpus_final=0,
            corpus_growth_pct=0.0,
            total_time_s=0.0,
            stage_smlgen_s=0.0,
            stage_afl_s=0.0,
            stage_diffcomp_s=0.0,
            plot_data=[],
        )
        with pytest.raises(AttributeError):
            metrics.campaign_id = "changed"  # type: ignore[misc]


# ---------------------------------------------------------------------------
# extract_campaign_metrics tests
# ---------------------------------------------------------------------------


class TestExtractCampaignMetrics:
    """Tests for extract_campaign_metrics function."""

    def test_complete_campaign_extracts_all_fields(self, tmp_path: Path) -> None:
        campaign_dir = tmp_path / "campaign_000"
        _create_complete_campaign(
            campaign_dir,
            bitmap_cvg=45.23,
            edges_found=1234,
            initial_corpus_files=100,
            queue_files=150,
            match_count=140,
            diff_count=8,
            failure_count=2,
        )

        metrics = extract_campaign_metrics(campaign_dir)
        assert metrics is not None
        assert metrics.campaign_id == "campaign_000"
        assert metrics.seed == 67890
        assert metrics.bitmap_cvg == pytest.approx(45.23)
        assert metrics.edges_found == 1234
        assert metrics.mismatch_count == 8
        assert metrics.mismatch_rate == pytest.approx(8 / (140 + 8))
        assert metrics.corpus_initial == 100
        assert metrics.corpus_final == 150
        assert metrics.corpus_growth_pct == pytest.approx(50.0)
        assert metrics.total_time_s == pytest.approx(330.0)
        assert metrics.stage_smlgen_s == pytest.approx(5.2)
        assert metrics.stage_afl_s == pytest.approx(300.1)
        assert metrics.stage_diffcomp_s == pytest.approx(24.7)
        assert len(metrics.plot_data) == 2

    def test_returns_none_for_missing_manifest(self, tmp_path: Path) -> None:
        campaign_dir = tmp_path / "campaign_001"
        campaign_dir.mkdir()
        result = extract_campaign_metrics(campaign_dir)
        assert result is None

    def test_returns_none_for_incomplete_status(self, tmp_path: Path) -> None:
        campaign_dir = tmp_path / "campaign_002"
        campaign_dir.mkdir()
        manifest = {"status": "running", "campaign_seed": 12345}
        (campaign_dir / "manifest.json").write_text(json.dumps(manifest))
        result = extract_campaign_metrics(campaign_dir)
        assert result is None

    def test_zero_diffcomp_files_gives_zero_mismatch_rate(
        self, tmp_path: Path
    ) -> None:
        campaign_dir = tmp_path / "campaign_003"
        _create_complete_campaign(
            campaign_dir,
            match_count=0,
            diff_count=0,
            failure_count=0,
        )
        metrics = extract_campaign_metrics(campaign_dir)
        assert metrics is not None
        assert metrics.mismatch_rate == pytest.approx(0.0)
        assert metrics.mismatch_count == 0

    def test_zero_initial_corpus_gives_zero_growth(self, tmp_path: Path) -> None:
        campaign_dir = tmp_path / "campaign_004"
        _create_complete_campaign(
            campaign_dir,
            initial_corpus_files=0,
            queue_files=10,
        )
        metrics = extract_campaign_metrics(campaign_dir)
        assert metrics is not None
        assert metrics.corpus_initial == 0
        assert metrics.corpus_growth_pct == pytest.approx(0.0)

    def test_excludes_dotfiles_and_readme_from_queue_count(
        self, tmp_path: Path
    ) -> None:
        campaign_dir = tmp_path / "campaign_005"
        _create_complete_campaign(
            campaign_dir,
            queue_files=10,
        )
        # _create_complete_campaign also adds .state and README.txt
        metrics = extract_campaign_metrics(campaign_dir)
        assert metrics is not None
        # Should count exactly 10 files (excluding .state and README.txt)
        assert metrics.corpus_final == 10

    def test_handles_missing_fuzzer_stats(self, tmp_path: Path) -> None:
        """Campaign with manifest but no fuzzer_stats should still extract."""
        campaign_dir = tmp_path / "campaign_006"
        campaign_dir.mkdir()

        manifest = {
            "status": "complete",
            "campaign_seed": 99999,
            "timing": {
                "duration_seconds": 100.0,
                "stages": {"smlgen": 1.0, "afl": 90.0, "diffcomp": 9.0},
            },
        }
        (campaign_dir / "manifest.json").write_text(json.dumps(manifest))

        # No afl_output or other dirs -- parsers should handle gracefully
        metrics = extract_campaign_metrics(campaign_dir)
        assert metrics is not None
        assert metrics.bitmap_cvg == pytest.approx(0.0)
        assert metrics.edges_found == 0

    def test_plot_data_included_in_metrics(self, tmp_path: Path) -> None:
        campaign_dir = tmp_path / "campaign_007"
        _create_complete_campaign(campaign_dir)
        metrics = extract_campaign_metrics(campaign_dir)
        assert metrics is not None
        assert len(metrics.plot_data) == 2
        assert "relative_time" in metrics.plot_data[0]
        assert "edges_found" in metrics.plot_data[0]


# ---------------------------------------------------------------------------
# collect_all_metrics tests
# ---------------------------------------------------------------------------


class TestCollectAllMetrics:
    """Tests for collect_all_metrics function."""

    def test_collects_complete_campaigns(self, tmp_path: Path) -> None:
        for i in range(3):
            _create_complete_campaign(
                tmp_path / f"campaign_{i:03d}",
                campaign_seed=1000 + i,
            )

        campaign_dirs = sorted(tmp_path.iterdir())
        metrics, skipped = collect_all_metrics(campaign_dirs)
        assert len(metrics) == 3
        assert len(skipped) == 0

    def test_skips_incomplete_campaigns(self, tmp_path: Path) -> None:
        # Complete campaign
        _create_complete_campaign(tmp_path / "campaign_000")

        # Incomplete campaign (no manifest)
        (tmp_path / "campaign_001").mkdir()

        # Incomplete campaign (running status)
        (tmp_path / "campaign_002").mkdir()
        (tmp_path / "campaign_002" / "manifest.json").write_text(
            json.dumps({"status": "running"})
        )

        campaign_dirs = sorted(tmp_path.iterdir())
        metrics, skipped = collect_all_metrics(campaign_dirs)
        assert len(metrics) == 1
        assert metrics[0].campaign_id == "campaign_000"
        assert sorted(skipped) == ["campaign_001", "campaign_002"]

    def test_returns_empty_for_no_campaigns(self) -> None:
        metrics, skipped = collect_all_metrics([])
        assert metrics == []
        assert skipped == []

    def test_returns_empty_for_all_incomplete(self, tmp_path: Path) -> None:
        (tmp_path / "campaign_000").mkdir()
        (tmp_path / "campaign_001").mkdir()
        campaign_dirs = sorted(tmp_path.iterdir())
        metrics, skipped = collect_all_metrics(campaign_dirs)
        assert len(metrics) == 0
        assert len(skipped) == 2
