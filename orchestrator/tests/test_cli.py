"""Tests for CLI argument parsing and subcommand registration."""

from __future__ import annotations

from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest
from click.testing import CliRunner

from polyfuzz_orchestrator.cli import cli
from polyfuzz_orchestrator.errors import PipelineError


class TestCLIHelp:
    """Test CLI help output."""

    def test_help_exits_zero(self):
        """polyfuzz --help exits 0 and shows usage."""
        runner = CliRunner()
        result = runner.invoke(cli, ["--help"])
        assert result.exit_code == 0
        assert "Usage" in result.output

    def test_help_shows_options(self):
        """polyfuzz --help shows all expected options."""
        runner = CliRunner()
        result = runner.invoke(cli, ["--help"])
        assert "--work-dir" in result.output
        assert "--tests" in result.output
        assert "--seed" in result.output
        assert "--afl-timeout" in result.output
        assert "--config" in result.output

    def test_help_shows_no_analytics_and_analyse(self):
        """polyfuzz --help shows --no-analytics flag and analyse subcommand."""
        runner = CliRunner()
        result = runner.invoke(cli, ["--help"])
        assert "--no-analytics" in result.output
        assert "analyse" in result.output

    def test_help_shows_subcommands(self):
        """polyfuzz --help shows run-stage and analyse subcommands."""
        runner = CliRunner()
        result = runner.invoke(cli, ["--help"])
        assert "run-stage" in result.output
        assert "analyse" in result.output


class TestCLISubcommands:
    """Test CLI subcommand routing to PipelineExecutor."""

    @pytest.mark.parametrize("stage", ["smlgen", "afl", "diffcomp", "coverage"])
    def test_run_stage_invokes_pipeline_with_stage(self, tmp_path: Path, stage: str):
        """polyfuzz -d <dir> run-stage <stage> invokes pipeline with only_stage=<stage>."""
        runner = CliRunner()

        mock_executor = MagicMock()
        mock_executor.run.return_value = []

        with patch("polyfuzz_orchestrator.cli.PipelineExecutor", return_value=mock_executor):
            result = runner.invoke(cli, ["-d", str(tmp_path), "run-stage", stage])

        assert result.exit_code == 0
        mock_executor.run.assert_called_once_with(only_stage=stage)

    def test_no_subcommand_invokes_campaign_orchestrator(self, tmp_path: Path):
        """polyfuzz -d /tmp/test with no subcommand runs CampaignOrchestrator."""
        runner = CliRunner()

        mock_orchestrator = MagicMock()
        mock_orchestrator.run.return_value = []

        with patch("polyfuzz_orchestrator.cli.CampaignOrchestrator", return_value=mock_orchestrator):
            result = runner.invoke(cli, ["-d", str(tmp_path)])

        assert result.exit_code == 0
        mock_orchestrator.run.assert_called_once_with(no_analytics=False)

    def test_work_dir_is_required(self):
        """polyfuzz with no --work-dir fails."""
        runner = CliRunner()
        result = runner.invoke(cli, [])
        assert result.exit_code != 0


class TestanalyseCLI:
    """Test the analyse subcommand."""

    def test_analyse_command_exists(self, tmp_path: Path):
        """polyfuzz analyse is a recognized subcommand (not 'No such command')."""
        runner = CliRunner()
        result = runner.invoke(cli, ["-d", str(tmp_path), "analyse"])
        assert "No such command" not in (result.output or "")

    def test_analyse_command_calls_run_analytics(self, tmp_path: Path):
        """polyfuzz analyse invokes run_analytics with the work_dir."""
        runner = CliRunner()

        with patch("polyfuzz_orchestrator.analytics.run_analytics") as mock_analytics:
            mock_analytics.return_value = MagicMock()
            result = runner.invoke(cli, ["-d", str(tmp_path), "analyse"])

        assert result.exit_code == 0
        mock_analytics.assert_called_once_with(tmp_path)

    def test_analyse_command_handles_no_campaigns(self, tmp_path: Path):
        """polyfuzz analyse on empty dir prints error and exits 1."""
        runner = CliRunner()

        with patch(
            "polyfuzz_orchestrator.analytics.run_analytics",
            side_effect=ValueError("No campaign directories found"),
        ):
            result = runner.invoke(cli, ["-d", str(tmp_path), "analyse"])

        assert result.exit_code == 1
        assert "Analytics error" in result.output

    def test_analyse_command_handles_generic_exception(self, tmp_path: Path):
        """polyfuzz analyse handles non-ValueError exceptions."""
        runner = CliRunner()

        with patch(
            "polyfuzz_orchestrator.analytics.run_analytics",
            side_effect=RuntimeError("unexpected"),
        ):
            result = runner.invoke(cli, ["-d", str(tmp_path), "analyse"])

        assert result.exit_code == 1
        assert "Analytics failed" in result.output


class TestNoAnalyticsFlag:
    """Test the --no-analytics flag on the default invocation."""

    def test_no_analytics_flag_accepted(self, tmp_path: Path):
        """polyfuzz --no-analytics is accepted without error."""
        runner = CliRunner()
        # Use --help to avoid actual execution
        result = runner.invoke(
            cli, ["-d", str(tmp_path), "--no-analytics", "--help"]
        )
        assert result.exit_code == 0

    def test_no_analytics_flag_passed_to_orchestrator(self, tmp_path: Path):
        """polyfuzz --campaigns 3 --no-analytics passes no_analytics=True to orchestrator."""
        runner = CliRunner()

        mock_orchestrator = MagicMock()
        mock_orchestrator.run.return_value = []

        with patch(
            "polyfuzz_orchestrator.cli.CampaignOrchestrator",
            return_value=mock_orchestrator,
        ):
            result = runner.invoke(
                cli,
                ["-d", str(tmp_path), "--campaigns", "3", "--no-analytics"],
            )

        assert result.exit_code == 0
        mock_orchestrator.run.assert_called_once_with(no_analytics=True)

    def test_multi_campaign_without_no_analytics_passes_false(self, tmp_path: Path):
        """polyfuzz --campaigns 3 (no --no-analytics) passes no_analytics=False."""
        runner = CliRunner()

        mock_orchestrator = MagicMock()
        mock_orchestrator.run.return_value = []

        with patch(
            "polyfuzz_orchestrator.cli.CampaignOrchestrator",
            return_value=mock_orchestrator,
        ):
            result = runner.invoke(
                cli,
                ["-d", str(tmp_path), "--campaigns", "3"],
            )

        assert result.exit_code == 0
        mock_orchestrator.run.assert_called_once_with(no_analytics=False)

    def test_single_campaign_uses_orchestrator_with_analytics(self, tmp_path: Path):
        """polyfuzz -d <dir> (single campaign, campaigns=1) uses CampaignOrchestrator with analytics."""
        runner = CliRunner()

        mock_orchestrator = MagicMock()
        mock_orchestrator.run.return_value = []

        with patch(
            "polyfuzz_orchestrator.cli.CampaignOrchestrator",
            return_value=mock_orchestrator,
        ):
            result = runner.invoke(cli, ["-d", str(tmp_path)])

        assert result.exit_code == 0
        mock_orchestrator.run.assert_called_once_with(no_analytics=False)


class TestCLIParameterVerification:
    """BILD-02: CLI accepts all documented parameters."""

    def test_all_documented_parameters_accepted(self, tmp_path: Path):
        """BILD-02: CLI accepts all documented parameters at once without error.

        Parameters tested: --work-dir, --tests, --seed, --campaigns,
        --afl-timeout, --config, --no-analytics.

        Note on AFL++ iteration count: The requirement text mentions
        'AFL++ iteration count' but the project intentionally uses time-based
        stopping (-V flag via --afl-timeout). AFL++ iteration count is
        intentionally not implemented; --afl-timeout provides time-based
        campaign control via AFL++ -V flag, which is superior for statistical
        reproducibility.
        """
        runner = CliRunner()

        # Create a dummy TOML config for --config
        toml_file = tmp_path / "polyfuzz.toml"
        toml_file.write_text("[polyfuzz]\n")

        mock_orchestrator = MagicMock()
        mock_orchestrator.run.return_value = []

        with patch(
            "polyfuzz_orchestrator.cli.CampaignOrchestrator",
            return_value=mock_orchestrator,
        ):
            result = runner.invoke(
                cli,
                [
                    "--work-dir", str(tmp_path),
                    "--tests", "50",
                    "--seed", "123",
                    "--campaigns", "1",
                    "--afl-timeout", "500",
                    "--config", str(toml_file),
                    "--no-analytics",
                ],
            )

        assert result.exit_code == 0, f"CLI failed: {result.output}"
        assert "Error" not in (result.output or "")

    def test_afl_timeout_maps_to_config(self, tmp_path: Path):
        """BILD-02: --afl-timeout value is forwarded to PipelineConfig.afl_timeout_s."""
        runner = CliRunner()

        captured_config = {}

        class ConfigCapturingOrchestrator:
            def __init__(self, cfg):
                captured_config["cfg"] = cfg

            def run(self, **kwargs):
                return []

        with patch(
            "polyfuzz_orchestrator.cli.CampaignOrchestrator",
            ConfigCapturingOrchestrator,
        ):
            result = runner.invoke(
                cli,
                ["-d", str(tmp_path), "--afl-timeout", "500"],
            )

        assert result.exit_code == 0
        assert captured_config["cfg"].afl_timeout_s == 500

    def test_pipeline_error_surfaces_stage_and_exit_code(self, tmp_path: Path):
        """PIPE-04: CLI surfaces stage name and exit code when pipeline fails."""
        runner = CliRunner()

        mock_orchestrator = MagicMock()
        mock_orchestrator.run.side_effect = PipelineError(
            stage_name="afl",
            exit_code=2,
            stderr="segfault",
            stdout="",
        )

        with patch(
            "polyfuzz_orchestrator.cli.CampaignOrchestrator",
            return_value=mock_orchestrator,
        ):
            result = runner.invoke(
                cli,
                ["-d", str(tmp_path)],
            )

        assert result.exit_code == 2, f"Expected exit code 2, got {result.exit_code}"
        assert "afl" in result.output, "CLI output must include stage name 'afl'"
