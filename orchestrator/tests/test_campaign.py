"""Tests for CampaignOrchestrator analytics integration.

Tests verify that analytics auto-runs after multi-campaign completion,
respects the no_analytics flag, and handles analytics errors gracefully.
"""

from __future__ import annotations

from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from polyfuzz_orchestrator.campaign import CampaignOrchestrator
from polyfuzz_orchestrator.config import PipelineConfig


def _make_config(tmp_path: Path, num_campaigns: int = 3) -> PipelineConfig:
    """Create a minimal PipelineConfig for testing."""
    return PipelineConfig(
        work_dir=tmp_path,
        tests_per_campaign=10,
        seed=42,
        num_campaigns=num_campaigns,
        afl_timeout_s=10,
        stage_timeout_s=20,
    )


# Common patches needed for CampaignOrchestrator.run() to not hit real I/O
_CAMPAIGN_PATCHES = [
    "polyfuzz_orchestrator.campaign.create_experiment_layout",
    "polyfuzz_orchestrator.campaign.write_experiment_manifest",
    "polyfuzz_orchestrator.campaign.collect_metadata",
    "polyfuzz_orchestrator.campaign.is_campaign_complete",
    "polyfuzz_orchestrator.campaign.update_experiment_manifest",
    "polyfuzz_orchestrator.campaign.build_campaign_manifest",
    "polyfuzz_orchestrator.campaign.write_manifest",
    "polyfuzz_orchestrator.campaign.PipelineExecutor",
]


class TestCampaignAnalyticsAutoRun:
    """Test that CampaignOrchestrator.run() auto-runs analytics by default."""

    def test_run_calls_analytics_by_default(self, tmp_path: Path):
        """When no_analytics is False (default), run_analytics is called after campaigns."""
        cfg = _make_config(tmp_path)
        orchestrator = CampaignOrchestrator(cfg)

        mock_executor = MagicMock()
        mock_executor.run.return_value = []

        with (
            patch("polyfuzz_orchestrator.campaign.create_experiment_layout"),
            patch("polyfuzz_orchestrator.campaign.write_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.collect_metadata", return_value={}),
            patch("polyfuzz_orchestrator.campaign.is_campaign_complete", return_value=False),
            patch("polyfuzz_orchestrator.campaign.update_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.build_campaign_manifest", return_value={}),
            patch("polyfuzz_orchestrator.campaign.write_manifest"),
            patch("polyfuzz_orchestrator.campaign.PipelineExecutor", return_value=mock_executor),
            patch("polyfuzz_orchestrator.analytics.run_analytics") as mock_analytics,
        ):
            result = orchestrator.run()

        mock_analytics.assert_called_once_with(tmp_path)
        assert len(result) == cfg.num_campaigns

    def test_run_skips_analytics_when_no_analytics_true(self, tmp_path: Path):
        """When no_analytics=True, run_analytics is NOT called."""
        cfg = _make_config(tmp_path)
        orchestrator = CampaignOrchestrator(cfg)

        mock_executor = MagicMock()
        mock_executor.run.return_value = []

        with (
            patch("polyfuzz_orchestrator.campaign.create_experiment_layout"),
            patch("polyfuzz_orchestrator.campaign.write_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.collect_metadata", return_value={}),
            patch("polyfuzz_orchestrator.campaign.is_campaign_complete", return_value=False),
            patch("polyfuzz_orchestrator.campaign.update_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.build_campaign_manifest", return_value={}),
            patch("polyfuzz_orchestrator.campaign.write_manifest"),
            patch("polyfuzz_orchestrator.campaign.PipelineExecutor", return_value=mock_executor),
            patch("polyfuzz_orchestrator.analytics.run_analytics") as mock_analytics,
        ):
            result = orchestrator.run(no_analytics=True)

        mock_analytics.assert_not_called()
        assert len(result) == cfg.num_campaigns


class TestCampaignAnalyticsErrorHandling:
    """Test that analytics errors are caught as warnings, not crashes."""

    def test_analytics_error_is_warning_not_crash(self, tmp_path: Path):
        """When run_analytics raises, orchestrator still returns completed dirs."""
        cfg = _make_config(tmp_path)
        orchestrator = CampaignOrchestrator(cfg)

        mock_executor = MagicMock()
        mock_executor.run.return_value = []

        with (
            patch("polyfuzz_orchestrator.campaign.create_experiment_layout"),
            patch("polyfuzz_orchestrator.campaign.write_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.collect_metadata", return_value={}),
            patch("polyfuzz_orchestrator.campaign.is_campaign_complete", return_value=False),
            patch("polyfuzz_orchestrator.campaign.update_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.build_campaign_manifest", return_value={}),
            patch("polyfuzz_orchestrator.campaign.write_manifest"),
            patch("polyfuzz_orchestrator.campaign.PipelineExecutor", return_value=mock_executor),
            patch(
                "polyfuzz_orchestrator.analytics.run_analytics",
                side_effect=ValueError("No campaign directories found"),
            ),
        ):
            # Should NOT raise -- analytics error is caught
            result = orchestrator.run()

        # All 3 campaigns should still be returned
        assert len(result) == cfg.num_campaigns

    def test_analytics_runtime_error_is_caught(self, tmp_path: Path):
        """Even RuntimeError from analytics is caught gracefully."""
        cfg = _make_config(tmp_path)
        orchestrator = CampaignOrchestrator(cfg)

        mock_executor = MagicMock()
        mock_executor.run.return_value = []

        with (
            patch("polyfuzz_orchestrator.campaign.create_experiment_layout"),
            patch("polyfuzz_orchestrator.campaign.write_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.collect_metadata", return_value={}),
            patch("polyfuzz_orchestrator.campaign.is_campaign_complete", return_value=False),
            patch("polyfuzz_orchestrator.campaign.update_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.build_campaign_manifest", return_value={}),
            patch("polyfuzz_orchestrator.campaign.write_manifest"),
            patch("polyfuzz_orchestrator.campaign.PipelineExecutor", return_value=mock_executor),
            patch(
                "polyfuzz_orchestrator.analytics.run_analytics",
                side_effect=RuntimeError("unexpected failure"),
            ),
        ):
            result = orchestrator.run()

        assert len(result) == cfg.num_campaigns

    def test_run_returns_completed_dirs_regardless_of_analytics(self, tmp_path: Path):
        """Return value is the list of completed campaign directories, not analytics result."""
        cfg = _make_config(tmp_path, num_campaigns=2)
        orchestrator = CampaignOrchestrator(cfg)

        mock_executor = MagicMock()
        mock_executor.run.return_value = []

        with (
            patch("polyfuzz_orchestrator.campaign.create_experiment_layout"),
            patch("polyfuzz_orchestrator.campaign.write_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.collect_metadata", return_value={}),
            patch("polyfuzz_orchestrator.campaign.is_campaign_complete", return_value=False),
            patch("polyfuzz_orchestrator.campaign.update_experiment_manifest"),
            patch("polyfuzz_orchestrator.campaign.build_campaign_manifest", return_value={}),
            patch("polyfuzz_orchestrator.campaign.write_manifest"),
            patch("polyfuzz_orchestrator.campaign.PipelineExecutor", return_value=mock_executor),
            patch("polyfuzz_orchestrator.analytics.run_analytics"),
        ):
            result = orchestrator.run()

        assert len(result) == 2
        # Each entry should be a campaign directory path
        for path in result:
            assert "campaign_" in str(path)
