"""Tests for analytics aggregator module.

Tests cover cross-campaign statistics (mean, median, stdev, 95% CI),
adaptive time grid construction, linear interpolation with clamping,
and growth curve aggregation across campaigns.
"""

from __future__ import annotations

import math

import pytest

from polyfuzz_orchestrator.analytics.aggregator import (
    GrowthCurveData,
    build_time_grid,
    compute_cross_campaign_stats,
    compute_metric_stats,
    interpolate_at,
    interpolate_growth_curves,
)
from polyfuzz_orchestrator.analytics.metrics import CampaignMetrics

# ---------------------------------------------------------------------------
# Fixtures: helper to build mock CampaignMetrics
# ---------------------------------------------------------------------------


def _make_metrics(
    campaign_id: str = "campaign_000",
    seed: int = 42,
    bitmap_cvg: float = 10.0,
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
    stage_coverage_s: float = 0.5,
    branch_total: int = 101,
    branch_covered: int = 80,
    branch_coverage_pct: float = 79.21,
    plot_data: list[dict[str, float]] | None = None,
) -> CampaignMetrics:
    """Build a CampaignMetrics with sensible defaults, overridable per-field."""
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
        stage_coverage_s=stage_coverage_s,
        branch_total=branch_total,
        branch_covered=branch_covered,
        branch_coverage_pct=branch_coverage_pct,
        plot_data=plot_data,
    )


# ---------------------------------------------------------------------------
# compute_metric_stats
# ---------------------------------------------------------------------------


class TestComputeMetricStats:
    """Tests for compute_metric_stats function."""

    def test_empty_list_returns_all_zeros(self) -> None:
        result = compute_metric_stats([])
        assert result == {
            "mean": 0.0,
            "median": 0.0,
            "stdev": 0.0,
            "ci_lower": 0.0,
            "ci_upper": 0.0,
        }

    def test_single_value_returns_value_with_zero_stdev(self) -> None:
        result = compute_metric_stats([42.0])
        assert result["mean"] == 42.0
        assert result["median"] == 42.0
        assert result["stdev"] == 0.0
        assert result["ci_lower"] == 42.0
        assert result["ci_upper"] == 42.0

    def test_two_values_symmetric_ci(self) -> None:
        result = compute_metric_stats([10.0, 20.0])
        assert result["mean"] == pytest.approx(15.0)
        assert result["median"] == pytest.approx(15.0)
        # CI should be symmetric around mean
        ci_width = result["ci_upper"] - result["ci_lower"]
        assert ci_width > 0
        midpoint = (result["ci_lower"] + result["ci_upper"]) / 2
        assert midpoint == pytest.approx(15.0)

    def test_three_values_known_statistics(self) -> None:
        values = [10.0, 20.0, 30.0]
        result = compute_metric_stats(values)
        assert result["mean"] == pytest.approx(20.0)
        assert result["median"] == pytest.approx(20.0)
        assert result["stdev"] == pytest.approx(10.0)
        # CI should contain the mean
        assert result["ci_lower"] < result["mean"]
        assert result["ci_upper"] > result["mean"]

    def test_ci_uses_t_distribution(self) -> None:
        """Verify CI width matches scipy t-distribution calculation."""
        from scipy.stats import t as t_dist

        values = [1.0, 2.0, 3.0, 4.0, 5.0]
        result = compute_metric_stats(values)
        n = len(values)
        m = sum(values) / n
        sd = (sum((x - m) ** 2 for x in values) / (n - 1)) ** 0.5
        se = sd / math.sqrt(n)
        expected_lo, expected_hi = t_dist.interval(confidence=0.95, df=n - 1, loc=m, scale=se)
        assert result["ci_lower"] == pytest.approx(expected_lo)
        assert result["ci_upper"] == pytest.approx(expected_hi)

    def test_all_same_values(self) -> None:
        result = compute_metric_stats([5.0, 5.0, 5.0])
        assert result["mean"] == 5.0
        assert result["stdev"] == 0.0
        # When stdev=0, CI should collapse to the mean
        assert result["ci_lower"] == pytest.approx(5.0)
        assert result["ci_upper"] == pytest.approx(5.0)


# ---------------------------------------------------------------------------
# compute_cross_campaign_stats
# ---------------------------------------------------------------------------


class TestComputeCrossCampaignStats:
    """Tests for compute_cross_campaign_stats function."""

    def test_computes_stats_for_all_numeric_fields(self) -> None:
        m1 = _make_metrics(bitmap_cvg=10.0, edges_found=100)
        m2 = _make_metrics(bitmap_cvg=20.0, edges_found=200)
        stats = compute_cross_campaign_stats([m1, m2])

        # Check all expected fields are present
        expected_fields = [
            "bitmap_cvg", "edges_found", "mismatch_count", "mismatch_rate",
            "corpus_initial", "corpus_final", "corpus_growth_pct",
            "total_time_s", "stage_smlgen_s", "stage_afl_s", "stage_diffcomp_s",
            "stage_coverage_s", "branch_coverage_pct",
        ]
        for field in expected_fields:
            assert field in stats, f"Missing field: {field}"
            assert "mean" in stats[field]
            assert "median" in stats[field]
            assert "stdev" in stats[field]
            assert "ci_lower" in stats[field]
            assert "ci_upper" in stats[field]

    def test_bitmap_cvg_stats_correct(self) -> None:
        m1 = _make_metrics(bitmap_cvg=10.0)
        m2 = _make_metrics(bitmap_cvg=20.0)
        m3 = _make_metrics(bitmap_cvg=30.0)
        stats = compute_cross_campaign_stats([m1, m2, m3])
        assert stats["bitmap_cvg"]["mean"] == pytest.approx(20.0)
        assert stats["bitmap_cvg"]["median"] == pytest.approx(20.0)

    def test_single_campaign(self) -> None:
        m = _make_metrics(edges_found=150)
        stats = compute_cross_campaign_stats([m])
        assert stats["edges_found"]["mean"] == 150.0
        assert stats["edges_found"]["stdev"] == 0.0

    def test_excludes_non_numeric_fields(self) -> None:
        m = _make_metrics()
        stats = compute_cross_campaign_stats([m])
        assert "campaign_id" not in stats
        assert "seed" not in stats  # seed is int but not a metric
        assert "plot_data" not in stats


# ---------------------------------------------------------------------------
# build_time_grid
# ---------------------------------------------------------------------------


class TestBuildTimeGrid:
    """Tests for build_time_grid function."""

    def test_short_duration_1s_step(self) -> None:
        grid = build_time_grid(30.0)
        assert grid[0] == 0.0
        assert grid[-1] == 30.0
        # Step should be 1s for <=60s
        assert len(grid) == 31
        assert grid[1] - grid[0] == pytest.approx(1.0)

    def test_medium_duration_5s_step(self) -> None:
        grid = build_time_grid(120.0)
        assert grid[0] == 0.0
        assert grid[-1] == 120.0
        assert grid[1] - grid[0] == pytest.approx(5.0)
        assert len(grid) == 25

    def test_long_duration_10s_step(self) -> None:
        grid = build_time_grid(900.0)
        assert grid[0] == 0.0
        assert grid[-1] == 900.0
        assert grid[1] - grid[0] == pytest.approx(10.0)
        assert len(grid) == 91

    def test_very_long_duration_30s_step(self) -> None:
        grid = build_time_grid(7200.0)
        assert grid[0] == 0.0
        assert grid[-1] == 7200.0
        assert grid[1] - grid[0] == pytest.approx(30.0)
        assert len(grid) == 241

    def test_boundary_60s_uses_1s_step(self) -> None:
        grid = build_time_grid(60.0)
        assert grid[1] - grid[0] == pytest.approx(1.0)
        assert len(grid) == 61

    def test_boundary_600s_uses_5s_step(self) -> None:
        grid = build_time_grid(600.0)
        assert grid[1] - grid[0] == pytest.approx(5.0)
        assert len(grid) == 121

    def test_boundary_3600s_uses_10s_step(self) -> None:
        grid = build_time_grid(3600.0)
        assert grid[1] - grid[0] == pytest.approx(10.0)
        assert len(grid) == 361

    def test_zero_duration_returns_single_point(self) -> None:
        grid = build_time_grid(0.0)
        assert grid == [0.0]

    def test_negative_duration_returns_single_point(self) -> None:
        grid = build_time_grid(-5.0)
        assert grid == [0.0]


# ---------------------------------------------------------------------------
# interpolate_at
# ---------------------------------------------------------------------------


class TestInterpolateAt:
    """Tests for interpolate_at function."""

    def test_exact_match(self) -> None:
        times = [0.0, 10.0, 20.0]
        values = [0.0, 50.0, 100.0]
        assert interpolate_at(times, values, 10.0) == pytest.approx(50.0)

    def test_midpoint_interpolation(self) -> None:
        times = [0.0, 10.0]
        values = [0.0, 100.0]
        assert interpolate_at(times, values, 5.0) == pytest.approx(50.0)

    def test_clamp_below_range(self) -> None:
        times = [10.0, 20.0]
        values = [100.0, 200.0]
        assert interpolate_at(times, values, 5.0) == pytest.approx(100.0)

    def test_clamp_above_range(self) -> None:
        times = [10.0, 20.0]
        values = [100.0, 200.0]
        assert interpolate_at(times, values, 25.0) == pytest.approx(200.0)

    def test_single_point(self) -> None:
        assert interpolate_at([5.0], [42.0], 0.0) == pytest.approx(42.0)
        assert interpolate_at([5.0], [42.0], 10.0) == pytest.approx(42.0)

    def test_quarter_interpolation(self) -> None:
        times = [0.0, 100.0]
        values = [0.0, 200.0]
        assert interpolate_at(times, values, 25.0) == pytest.approx(50.0)


# ---------------------------------------------------------------------------
# interpolate_growth_curves
# ---------------------------------------------------------------------------


class TestInterpolateGrowthCurves:
    """Tests for interpolate_growth_curves function."""

    def test_two_campaigns_different_durations(self) -> None:
        """Campaign 0 runs 60s, campaign 1 runs 30s -- should forward-fill."""
        m0 = _make_metrics(
            campaign_id="campaign_000",
            plot_data=[
                {"relative_time": 0.0, "edges_found": 0.0, "map_size": 0.0,
                 "total_execs": 0.0, "total_crashes": 0.0, "corpus_count": 5.0},
                {"relative_time": 30.0, "edges_found": 50.0, "map_size": 5.0,
                 "total_execs": 500.0, "total_crashes": 0.0, "corpus_count": 10.0},
                {"relative_time": 60.0, "edges_found": 100.0, "map_size": 10.0,
                 "total_execs": 1000.0, "total_crashes": 1.0, "corpus_count": 15.0},
            ],
        )
        m1 = _make_metrics(
            campaign_id="campaign_001",
            plot_data=[
                {"relative_time": 0.0, "edges_found": 0.0, "map_size": 0.0,
                 "total_execs": 0.0, "total_crashes": 0.0, "corpus_count": 8.0},
                {"relative_time": 30.0, "edges_found": 80.0, "map_size": 8.0,
                 "total_execs": 800.0, "total_crashes": 2.0, "corpus_count": 18.0},
            ],
        )

        curves = interpolate_growth_curves([m0, m1])

        # Should have all tracked metrics
        assert "edges_found" in curves
        assert "bitmap_cvg" in curves
        assert "total_execs" in curves
        assert "total_crashes" in curves
        assert "corpus_count" in curves

        edges = curves["edges_found"]
        assert isinstance(edges, GrowthCurveData)

        # Time grid should be 1s step for max 60s
        assert edges.time_grid[0] == 0.0
        assert edges.time_grid[-1] == 60.0
        assert edges.time_grid[1] - edges.time_grid[0] == pytest.approx(1.0)

        # At t=0, both campaigns have edges=0
        assert edges.per_campaign["campaign_000"][0] == pytest.approx(0.0)
        assert edges.per_campaign["campaign_001"][0] == pytest.approx(0.0)

        # At t=30, campaign_000=50, campaign_001=80
        t30_idx = edges.time_grid.index(30.0)
        assert edges.per_campaign["campaign_000"][t30_idx] == pytest.approx(50.0)
        assert edges.per_campaign["campaign_001"][t30_idx] == pytest.approx(80.0)

        # At t=60, campaign_000=100, campaign_001 forward-filled=80
        t60_idx = edges.time_grid.index(60.0)
        assert edges.per_campaign["campaign_000"][t60_idx] == pytest.approx(100.0)
        assert edges.per_campaign["campaign_001"][t60_idx] == pytest.approx(80.0)

        # Mean and CI should be computed
        assert len(edges.mean) == len(edges.time_grid)
        assert len(edges.ci_lower) == len(edges.time_grid)
        assert len(edges.ci_upper) == len(edges.time_grid)

        # Mean at t=30 should be (50+80)/2 = 65
        assert edges.mean[t30_idx] == pytest.approx(65.0)

    def test_campaign_count_tracks_active_campaigns(self) -> None:
        """campaign_count should reflect how many campaigns have data at each point."""
        m0 = _make_metrics(
            campaign_id="campaign_000",
            plot_data=[
                {"relative_time": 0.0, "edges_found": 0.0, "map_size": 0.0,
                 "total_execs": 0.0, "total_crashes": 0.0, "corpus_count": 5.0},
                {"relative_time": 60.0, "edges_found": 100.0, "map_size": 10.0,
                 "total_execs": 1000.0, "total_crashes": 0.0, "corpus_count": 15.0},
            ],
        )
        m1 = _make_metrics(
            campaign_id="campaign_001",
            plot_data=[
                {"relative_time": 0.0, "edges_found": 0.0, "map_size": 0.0,
                 "total_execs": 0.0, "total_crashes": 0.0, "corpus_count": 8.0},
                {"relative_time": 30.0, "edges_found": 80.0, "map_size": 8.0,
                 "total_execs": 800.0, "total_crashes": 0.0, "corpus_count": 12.0},
            ],
        )

        curves = interpolate_growth_curves([m0, m1])
        edges = curves["edges_found"]

        # At t=0, both have data -> count=2
        assert edges.campaign_count[0] == 2
        # At t=30, both still have data (campaign_001 ends at 30) -> count=2
        t30_idx = edges.time_grid.index(30.0)
        assert edges.campaign_count[t30_idx] == 2
        # At t=60, only campaign_000 has data beyond 30 -> count=1
        t60_idx = edges.time_grid.index(60.0)
        assert edges.campaign_count[t60_idx] == 1

    def test_bitmap_cvg_uses_map_size_column(self) -> None:
        """bitmap_cvg growth curve should read from map_size in plot_data."""
        m = _make_metrics(
            campaign_id="campaign_000",
            plot_data=[
                {"relative_time": 0.0, "edges_found": 0.0, "map_size": 1.5,
                 "total_execs": 0.0, "total_crashes": 0.0, "corpus_count": 5.0},
                {"relative_time": 10.0, "edges_found": 10.0, "map_size": 3.5,
                 "total_execs": 100.0, "total_crashes": 0.0, "corpus_count": 8.0},
            ],
        )
        curves = interpolate_growth_curves([m])
        cvg = curves["bitmap_cvg"]
        assert cvg.per_campaign["campaign_000"][0] == pytest.approx(1.5)
        t10_idx = cvg.time_grid.index(10.0)
        assert cvg.per_campaign["campaign_000"][t10_idx] == pytest.approx(3.5)

    def test_empty_plot_data_campaigns_excluded(self) -> None:
        """Campaigns with empty plot_data should be excluded from curves."""
        m0 = _make_metrics(campaign_id="campaign_000", plot_data=[])
        m1 = _make_metrics(
            campaign_id="campaign_001",
            plot_data=[
                {"relative_time": 0.0, "edges_found": 10.0, "map_size": 1.0,
                 "total_execs": 100.0, "total_crashes": 0.0, "corpus_count": 5.0},
                {"relative_time": 10.0, "edges_found": 20.0, "map_size": 2.0,
                 "total_execs": 200.0, "total_crashes": 0.0, "corpus_count": 10.0},
            ],
        )
        curves = interpolate_growth_curves([m0, m1])
        edges = curves["edges_found"]
        # Only campaign_001 should be in per_campaign
        assert "campaign_001" in edges.per_campaign
        assert "campaign_000" not in edges.per_campaign

    def test_all_empty_plot_data_returns_empty_curves(self) -> None:
        """If all campaigns have empty plot_data, return empty curves dict."""
        m0 = _make_metrics(campaign_id="campaign_000", plot_data=[])
        m1 = _make_metrics(campaign_id="campaign_001", plot_data=[])
        curves = interpolate_growth_curves([m0, m1])
        assert curves == {}
