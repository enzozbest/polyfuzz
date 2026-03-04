"""Tests for PipelineExecutor -- full pipeline and single-stage execution."""

from __future__ import annotations

import contextlib
import stat
from pathlib import Path
from unittest.mock import patch

import pytest

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PipelineError, PreflightError
from polyfuzz_orchestrator.process import StageResult

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_config(tmp_path: Path, **overrides) -> PipelineConfig:
    """Create a PipelineConfig with sensible test defaults rooted in tmp_path."""
    defaults = {
        "work_dir": tmp_path,
        "smlgen_jar": tmp_path / "smlgen.jar",
        "polylex_bin": tmp_path / "polylex_fuzz",
        "diffcomp_bin": tmp_path / "diffcomp",
        "afl_fuzz_bin": tmp_path / "afl-fuzz",
        "afl_timeout_s": 300,
        "stage_timeout_s": 600,
        "tests_per_campaign": 100,
        "seed": 42,
    }
    defaults.update(overrides)
    return PipelineConfig(**defaults)


def _make_fake_binary(path: Path) -> None:
    """Create a fake executable file."""
    path.write_text("#!/bin/sh\n")
    path.chmod(path.stat().st_mode | stat.S_IEXEC | stat.S_IXGRP | stat.S_IXOTH)


def _make_successful_result(stage_name: str, output_dir: Path) -> StageResult:
    """Create a successful StageResult for mocking."""
    return StageResult(
        stage_name=stage_name,
        exit_code=0,
        duration_seconds=1.0,
        stdout=f"{stage_name} completed",
        stderr="",
        output_dir=output_dir,
    )


def _make_failed_result(stage_name: str, output_dir: Path) -> StageResult:
    """Create a failed StageResult for mocking."""
    return StageResult(
        stage_name=stage_name,
        exit_code=1,
        duration_seconds=0.5,
        stdout="",
        stderr=f"{stage_name} failed: some error",
        output_dir=output_dir,
    )


# ===========================================================================
# PipelineExecutor tests
# ===========================================================================


class TestPipelineExecutorFullPipeline:
    """Test PipelineExecutor.run() with no only_stage (full pipeline)."""

    def test_runs_all_three_stages_in_order(self, tmp_path: Path):
        """PipelineExecutor.run() with no only_stage executes all 3 stages in order."""
        from polyfuzz_orchestrator.pipeline import PipelineExecutor

        config = _make_config(tmp_path)
        executor = PipelineExecutor(config)

        executed_stages: list[str] = []

        def mock_validate(campaign_dir, cfg):
            pass

        def make_mock_execute(stage_name):
            def mock_execute(campaign_dir, cfg, runner):
                executed_stages.append(stage_name)
                return _make_successful_result(stage_name, campaign_dir)
            return mock_execute

        with (
            patch.object(executor._stages["smlgen"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["afl"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["diffcomp"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["smlgen"], "execute", side_effect=make_mock_execute("smlgen")),
            patch.object(executor._stages["afl"], "execute", side_effect=make_mock_execute("afl")),
            patch.object(executor._stages["diffcomp"], "execute", side_effect=make_mock_execute("diffcomp")),
            patch("polyfuzz_orchestrator.pipeline.verify_components", return_value=[]),
            patch("polyfuzz_orchestrator.pipeline.create_campaign_layout", return_value={}),
        ):
            results = executor.run()

        assert executed_stages == ["smlgen", "afl", "diffcomp"]
        assert len(results) == 3

    def test_returns_list_of_stage_results(self, tmp_path: Path):
        """PipelineExecutor.run() returns list of StageResults for all completed stages."""
        from polyfuzz_orchestrator.pipeline import PipelineExecutor

        config = _make_config(tmp_path)
        executor = PipelineExecutor(config)

        def mock_validate(campaign_dir, cfg):
            pass

        def make_mock_execute(stage_name):
            def mock_execute(campaign_dir, cfg, runner):
                return _make_successful_result(stage_name, campaign_dir)
            return mock_execute

        with (
            patch.object(executor._stages["smlgen"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["afl"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["diffcomp"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["smlgen"], "execute", side_effect=make_mock_execute("smlgen")),
            patch.object(executor._stages["afl"], "execute", side_effect=make_mock_execute("afl")),
            patch.object(executor._stages["diffcomp"], "execute", side_effect=make_mock_execute("diffcomp")),
            patch("polyfuzz_orchestrator.pipeline.verify_components", return_value=[]),
            patch("polyfuzz_orchestrator.pipeline.create_campaign_layout", return_value={}),
        ):
            results = executor.run()

        assert all(isinstance(r, StageResult) for r in results)
        assert [r.stage_name for r in results] == ["smlgen", "afl", "diffcomp"]


class TestPipelineExecutorSingleStage:
    """Test PipelineExecutor.run(only_stage=...) for each stage."""

    @pytest.mark.parametrize("stage_name", ["smlgen", "afl", "diffcomp"])
    def test_runs_only_specified_stage(self, tmp_path: Path, stage_name: str):
        """PipelineExecutor.run(only_stage=X) executes only that stage."""
        from polyfuzz_orchestrator.pipeline import PipelineExecutor

        config = _make_config(tmp_path)
        executor = PipelineExecutor(config)

        executed_stages: list[str] = []

        def mock_validate(campaign_dir, cfg):
            pass

        def make_mock_execute(sn):
            def mock_execute(campaign_dir, cfg, runner):
                executed_stages.append(sn)
                return _make_successful_result(sn, campaign_dir)
            return mock_execute

        # Patch all stages using ExitStack for dynamic list of patches
        patches = []
        for sn in ["smlgen", "afl", "diffcomp"]:
            patches.append(patch.object(executor._stages[sn], "validate", side_effect=mock_validate))
            patches.append(patch.object(executor._stages[sn], "execute", side_effect=make_mock_execute(sn)))
        patches.append(patch("polyfuzz_orchestrator.pipeline.verify_components", return_value=[]))
        patches.append(patch("polyfuzz_orchestrator.pipeline.create_campaign_layout", return_value={}))

        with contextlib.ExitStack() as stack:
            for p in patches:
                stack.enter_context(p)
            results = executor.run(only_stage=stage_name)

        assert executed_stages == [stage_name]
        assert len(results) == 1
        assert results[0].stage_name == stage_name


class TestPipelineExecutorFailure:
    """Test PipelineExecutor failure handling."""

    def test_raises_pipeline_error_on_nonzero_exit(self, tmp_path: Path):
        """PipelineExecutor.run() raises PipelineError when a stage returns non-zero."""
        from polyfuzz_orchestrator.pipeline import PipelineExecutor

        config = _make_config(tmp_path)
        executor = PipelineExecutor(config)

        def mock_validate(campaign_dir, cfg):
            pass

        def mock_smlgen_execute(campaign_dir, cfg, runner):
            return _make_failed_result("smlgen", campaign_dir)

        with (
            patch.object(executor._stages["smlgen"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["smlgen"], "execute", side_effect=mock_smlgen_execute),
            patch("polyfuzz_orchestrator.pipeline.verify_components", return_value=[]),
            patch("polyfuzz_orchestrator.pipeline.create_campaign_layout", return_value={}),
        ):
            with pytest.raises(PipelineError) as exc_info:
                executor.run()

        assert exc_info.value.stage_name == "smlgen"
        assert exc_info.value.exit_code == 1

    def test_stops_after_first_failed_stage(self, tmp_path: Path):
        """PipelineExecutor.run() stops execution after first failed stage."""
        from polyfuzz_orchestrator.pipeline import PipelineExecutor

        config = _make_config(tmp_path)
        executor = PipelineExecutor(config)

        executed_stages: list[str] = []

        def mock_validate(campaign_dir, cfg):
            pass

        def mock_smlgen_fail(campaign_dir, cfg, runner):
            executed_stages.append("smlgen")
            return _make_failed_result("smlgen", campaign_dir)

        def mock_afl_execute(campaign_dir, cfg, runner):
            executed_stages.append("afl")
            return _make_successful_result("afl", campaign_dir)

        with (
            patch.object(executor._stages["smlgen"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["afl"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["diffcomp"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["smlgen"], "execute", side_effect=mock_smlgen_fail),
            patch.object(executor._stages["afl"], "execute", side_effect=mock_afl_execute),
            patch.object(executor._stages["diffcomp"], "execute", side_effect=lambda *a: _make_successful_result("diffcomp", tmp_path)),
            patch("polyfuzz_orchestrator.pipeline.verify_components", return_value=[]),
            patch("polyfuzz_orchestrator.pipeline.create_campaign_layout", return_value={}),
        ):
            with pytest.raises(PipelineError):
                executor.run()

        # Only smlgen should have been executed, not afl/diffcomp
        assert executed_stages == ["smlgen"]


class TestPipelineExecutorPreflight:
    """Test PipelineExecutor pre-flight checks."""

    def test_calls_verify_components_before_stages(self, tmp_path: Path):
        """PipelineExecutor.run() calls verify_components before any stage execution."""
        from polyfuzz_orchestrator.pipeline import PipelineExecutor

        config = _make_config(tmp_path)
        executor = PipelineExecutor(config)

        call_order: list[str] = []

        def mock_verify(cfg):
            call_order.append("verify")
            return []

        def mock_validate(campaign_dir, cfg):
            pass

        def make_mock_execute(stage_name):
            def mock_execute(campaign_dir, cfg, runner):
                call_order.append(f"execute_{stage_name}")
                return _make_successful_result(stage_name, campaign_dir)
            return mock_execute

        with (
            patch.object(executor._stages["smlgen"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["afl"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["diffcomp"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["smlgen"], "execute", side_effect=make_mock_execute("smlgen")),
            patch.object(executor._stages["afl"], "execute", side_effect=make_mock_execute("afl")),
            patch.object(executor._stages["diffcomp"], "execute", side_effect=make_mock_execute("diffcomp")),
            patch("polyfuzz_orchestrator.pipeline.verify_components", side_effect=mock_verify),
            patch("polyfuzz_orchestrator.pipeline.create_campaign_layout", return_value={}),
        ):
            executor.run()

        assert call_order[0] == "verify"
        assert "execute_smlgen" in call_order

    def test_raises_preflight_error_when_components_missing(self, tmp_path: Path):
        """PipelineExecutor.run() raises PreflightError when verify_components finds issues."""
        from polyfuzz_orchestrator.pipeline import PipelineExecutor

        config = _make_config(tmp_path)
        executor = PipelineExecutor(config)

        with (
            patch("polyfuzz_orchestrator.pipeline.verify_components", return_value=["java not found"]),
        ):
            with pytest.raises(PreflightError):
                executor.run()


class TestPipelineExecutorLayout:
    """Test PipelineExecutor campaign directory creation."""

    def test_creates_campaign_layout_before_running_stages(self, tmp_path: Path):
        """PipelineExecutor.run() creates campaign directory layout before running stages."""
        from polyfuzz_orchestrator.pipeline import PipelineExecutor

        config = _make_config(tmp_path)
        executor = PipelineExecutor(config)

        call_order: list[str] = []

        def mock_layout(work_dir):
            call_order.append("layout")
            return {}

        def mock_validate(campaign_dir, cfg):
            pass

        def mock_execute(campaign_dir, cfg, runner):
            call_order.append("execute")
            return _make_successful_result("smlgen", campaign_dir)

        with (
            patch.object(executor._stages["smlgen"], "validate", side_effect=mock_validate),
            patch.object(executor._stages["smlgen"], "execute", side_effect=mock_execute),
            patch("polyfuzz_orchestrator.pipeline.verify_components", return_value=[]),
            patch("polyfuzz_orchestrator.pipeline.create_campaign_layout", side_effect=mock_layout),
        ):
            executor.run(only_stage="smlgen")

        assert call_order.index("layout") < call_order.index("execute")
