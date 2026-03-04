"""Tests for pipeline stage implementations."""

from __future__ import annotations

import stat
from pathlib import Path
from unittest.mock import MagicMock

import pytest

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PreflightError
from polyfuzz_orchestrator.process import ProcessRunner, StageResult

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_config(tmp_path: Path, **overrides) -> PipelineConfig:
    """Create a PipelineConfig with sensible test defaults rooted in tmp_path."""
    defaults = {
        "work_dir": tmp_path,
        "smlgen_bin": tmp_path / "smlgen_bin",
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


def _make_campaign_dirs(tmp_path: Path) -> dict[str, Path]:
    """Create a campaign directory layout."""
    dirs = {
        "corpus": tmp_path / "corpus",
        "afl_output": tmp_path / "afl_output",
        "diffcomp_input": tmp_path / "diffcomp_input",
        "diffcomp_output": tmp_path / "diffcomp_output",
        "results": tmp_path / "results",
    }
    for d in dirs.values():
        d.mkdir(parents=True, exist_ok=True)
    return dirs


# ===========================================================================
# Stage ABC tests
# ===========================================================================

class TestStageABC:
    """Test that Stage ABC requires subclasses to implement name, validate, execute."""

    def test_stage_cannot_be_instantiated(self):
        from polyfuzz_orchestrator.stages.base import Stage
        with pytest.raises(TypeError):
            Stage()  # type: ignore[abstract]

    def test_subclass_must_implement_name(self):
        from polyfuzz_orchestrator.stages.base import Stage

        class Incomplete(Stage):
            def validate(self, campaign_dir: Path, config: PipelineConfig) -> None:
                pass

            def execute(
                self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
            ) -> StageResult:
                return StageResult("x", 0, 0.0, "", "", campaign_dir)

        with pytest.raises(TypeError):
            Incomplete()  # type: ignore[abstract]

    def test_subclass_must_implement_validate(self):
        from polyfuzz_orchestrator.stages.base import Stage

        class Incomplete(Stage):
            @property
            def name(self) -> str:
                return "test"

            def execute(
                self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
            ) -> StageResult:
                return StageResult("x", 0, 0.0, "", "", campaign_dir)

        with pytest.raises(TypeError):
            Incomplete()  # type: ignore[abstract]

    def test_subclass_must_implement_execute(self):
        from polyfuzz_orchestrator.stages.base import Stage

        class Incomplete(Stage):
            @property
            def name(self) -> str:
                return "test"

            def validate(self, campaign_dir: Path, config: PipelineConfig) -> None:
                pass

        with pytest.raises(TypeError):
            Incomplete()  # type: ignore[abstract]


# ===========================================================================
# SmlgenStage tests
# ===========================================================================

class TestSmlgenStage:
    """Tests for SmlgenStage validation and execution."""

    def test_validate_raises_if_binary_missing(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.smlgen import SmlgenStage

        config = _make_config(tmp_path)
        # Do NOT create smlgen_bin
        _make_campaign_dirs(tmp_path)

        stage = SmlgenStage()
        with pytest.raises(PreflightError):
            stage.validate(tmp_path, config)

    def test_validate_passes_with_binary_present(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.smlgen import SmlgenStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.smlgen_bin)
        _make_campaign_dirs(tmp_path)

        stage = SmlgenStage()
        # Should not raise
        stage.validate(tmp_path, config)

    def test_execute_calls_runner_with_correct_command(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.smlgen import SmlgenStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.smlgen_bin)
        dirs = _make_campaign_dirs(tmp_path)

        runner = MagicMock(spec=ProcessRunner)
        expected_result = StageResult(
            stage_name="smlgen",
            exit_code=0,
            duration_seconds=1.0,
            stdout="generated 100 files",
            stderr="",
            output_dir=dirs["corpus"],
        )
        runner.run.return_value = expected_result

        stage = SmlgenStage()
        result = stage.execute(tmp_path, config, runner)

        # Verify runner was called
        runner.run.assert_called_once()
        call_args = runner.run.call_args

        # Command invokes the smlgen wrapper script directly with CLI args
        cmd = call_args[1]["cmd"] if "cmd" in call_args[1] else call_args[0][0]
        assert cmd[0] == str(config.smlgen_bin)
        assert "-n" in cmd
        assert str(config.tests_per_campaign) in cmd
        assert "-o" in cmd
        assert str(dirs["corpus"]) in cmd

        # Stage name should be "smlgen"
        stage_name = call_args[1].get("stage_name", call_args[0][1] if len(call_args[0]) > 1 else None)
        assert stage_name == "smlgen"

        assert result == expected_result

    def test_execute_passes_seed_when_configured(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.smlgen import SmlgenStage

        config = _make_config(tmp_path, seed=42)
        _make_fake_binary(config.smlgen_bin)
        _make_campaign_dirs(tmp_path)

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="smlgen",
            exit_code=0,
            duration_seconds=0.5,
            stdout="",
            stderr="",
            output_dir=tmp_path / "corpus",
        )

        stage = SmlgenStage()
        stage.execute(tmp_path, config, runner)

        cmd = runner.run.call_args[1]["cmd"]
        assert "-seed" in cmd
        assert "42" in cmd

    def test_execute_omits_seed_when_none(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.smlgen import SmlgenStage

        config = _make_config(tmp_path, seed=None)
        _make_fake_binary(config.smlgen_bin)
        _make_campaign_dirs(tmp_path)

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="smlgen",
            exit_code=0,
            duration_seconds=0.5,
            stdout="",
            stderr="",
            output_dir=tmp_path / "corpus",
        )

        stage = SmlgenStage()
        stage.execute(tmp_path, config, runner)

        cmd = runner.run.call_args[1]["cmd"]
        assert "-seed" not in cmd

    def test_execute_returns_stage_result_with_correct_name(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.smlgen import SmlgenStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.smlgen_bin)
        _make_campaign_dirs(tmp_path)

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="smlgen",
            exit_code=0,
            duration_seconds=0.5,
            stdout="",
            stderr="",
            output_dir=tmp_path / "corpus",
        )

        stage = SmlgenStage()
        result = stage.execute(tmp_path, config, runner)
        assert result.stage_name == "smlgen"


# ===========================================================================
# AflStage tests
# ===========================================================================

class TestAflStage:
    """Tests for AflStage validation and execution."""

    def test_validate_raises_if_afl_binary_missing(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.afl import AflStage

        config = _make_config(tmp_path)
        # Do NOT create afl_fuzz_bin
        _make_fake_binary(config.polylex_bin)
        dirs = _make_campaign_dirs(tmp_path)
        # Create a corpus file so corpus is non-empty
        (dirs["corpus"] / "test.sml").write_text("val x = 1")

        stage = AflStage()
        with pytest.raises(PreflightError):
            stage.validate(tmp_path, config)

    def test_validate_raises_if_corpus_empty(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.afl import AflStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.afl_fuzz_bin)
        _make_fake_binary(config.polylex_bin)
        _make_campaign_dirs(tmp_path)
        # corpus dir exists but is empty

        stage = AflStage()
        with pytest.raises(PreflightError):
            stage.validate(tmp_path, config)

    def test_validate_passes_when_all_present(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.afl import AflStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.afl_fuzz_bin)
        _make_fake_binary(config.polylex_bin)
        dirs = _make_campaign_dirs(tmp_path)
        (dirs["corpus"] / "test.sml").write_text("val x = 1")

        stage = AflStage()
        # Should not raise
        stage.validate(tmp_path, config)

    def test_execute_calls_runner_with_correct_afl_command(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.afl import AflStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.afl_fuzz_bin)
        _make_fake_binary(config.polylex_bin)
        dirs = _make_campaign_dirs(tmp_path)
        (dirs["corpus"] / "test.sml").write_text("val x = 1")

        runner = MagicMock(spec=ProcessRunner)
        expected_result = StageResult(
            stage_name="afl",
            exit_code=0,
            duration_seconds=300.0,
            stdout="",
            stderr="",
            output_dir=dirs["afl_output"],
        )
        runner.run.return_value = expected_result

        stage = AflStage()
        result = stage.execute(tmp_path, config, runner)

        runner.run.assert_called_once()
        call_args = runner.run.call_args
        cmd = call_args[1]["cmd"] if "cmd" in call_args[1] else call_args[0][0]

        # Should contain afl-fuzz binary path
        assert str(config.afl_fuzz_bin) in cmd[0]
        # Should have -i (corpus), -o (output), -V (timeout)
        assert "-i" in cmd
        assert "-o" in cmd
        assert "-V" in cmd
        # Should end with -- polylex_bin @@
        assert "--" in cmd
        assert str(config.polylex_bin) in cmd
        assert "@@" in cmd

        assert result == expected_result

    def test_execute_sets_afl_environment_variables(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.afl import AflStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.afl_fuzz_bin)
        _make_fake_binary(config.polylex_bin)
        dirs = _make_campaign_dirs(tmp_path)
        (dirs["corpus"] / "test.sml").write_text("val x = 1")

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="afl",
            exit_code=0,
            duration_seconds=300.0,
            stdout="",
            stderr="",
            output_dir=dirs["afl_output"],
        )

        stage = AflStage()
        stage.execute(tmp_path, config, runner)

        call_args = runner.run.call_args
        env = call_args[1].get("env", call_args[0][4] if len(call_args[0]) > 4 else None)
        assert env is not None
        assert env["AFL_SKIP_CPUFREQ"] == "1"
        assert env["AFL_I_DONT_CARE_ABOUT_MISSING_CRASHES"] == "1"
        assert env["AFL_NO_UI"] == "1"

    def test_execute_omits_t_flag_when_exec_timeout_is_none(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.afl import AflStage

        config = _make_config(tmp_path, afl_exec_timeout_ms=None)
        _make_fake_binary(config.afl_fuzz_bin)
        _make_fake_binary(config.polylex_bin)
        dirs = _make_campaign_dirs(tmp_path)
        (dirs["corpus"] / "test.sml").write_text("val x = 1")

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="afl", exit_code=0, duration_seconds=1.0,
            stdout="", stderr="", output_dir=dirs["afl_output"],
        )

        stage = AflStage()
        stage.execute(tmp_path, config, runner)

        call_args = runner.run.call_args
        cmd = call_args[1]["cmd"] if "cmd" in call_args[1] else call_args[0][0]
        assert "-t" not in cmd

    def test_execute_includes_t_flag_when_exec_timeout_set(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.afl import AflStage

        config = _make_config(tmp_path, afl_exec_timeout_ms=5000)
        _make_fake_binary(config.afl_fuzz_bin)
        _make_fake_binary(config.polylex_bin)
        dirs = _make_campaign_dirs(tmp_path)
        (dirs["corpus"] / "test.sml").write_text("val x = 1")

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="afl", exit_code=0, duration_seconds=1.0,
            stdout="", stderr="", output_dir=dirs["afl_output"],
        )

        stage = AflStage()
        stage.execute(tmp_path, config, runner)

        call_args = runner.run.call_args
        cmd = call_args[1]["cmd"] if "cmd" in call_args[1] else call_args[0][0]
        t_idx = cmd.index("-t")
        assert cmd[t_idx + 1] == "5000"


# ===========================================================================
# DiffcompStage tests
# ===========================================================================

class TestDiffcompStage:
    """Tests for DiffcompStage validation and execution."""

    def test_validate_raises_if_diffcomp_bin_missing(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

        config = _make_config(tmp_path)
        # Do NOT create diffcomp_bin
        dirs = _make_campaign_dirs(tmp_path)
        queue_dir = dirs["afl_output"] / "default" / "queue"
        queue_dir.mkdir(parents=True)
        (queue_dir / "id:000000").write_text("val x = 1")

        stage = DiffcompStage()
        with pytest.raises(PreflightError):
            stage.validate(tmp_path, config)

    def test_validate_raises_if_diffcomp_bin_not_executable(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

        config = _make_config(tmp_path)
        # Create diffcomp_bin but NOT executable
        config.diffcomp_bin.write_text("#!/bin/sh\n")
        dirs = _make_campaign_dirs(tmp_path)
        queue_dir = dirs["afl_output"] / "default" / "queue"
        queue_dir.mkdir(parents=True)
        (queue_dir / "id:000000").write_text("val x = 1")

        stage = DiffcompStage()
        with pytest.raises(PreflightError):
            stage.validate(tmp_path, config)

    def test_validate_raises_if_queue_dir_missing(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.diffcomp_bin)
        _make_campaign_dirs(tmp_path)
        # Do NOT create queue dir

        stage = DiffcompStage()
        with pytest.raises(PreflightError):
            stage.validate(tmp_path, config)

    def test_validate_raises_if_queue_dir_empty(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.diffcomp_bin)
        dirs = _make_campaign_dirs(tmp_path)
        queue_dir = dirs["afl_output"] / "default" / "queue"
        queue_dir.mkdir(parents=True)
        # Empty queue directory

        stage = DiffcompStage()
        with pytest.raises(PreflightError):
            stage.validate(tmp_path, config)

    def test_validate_passes_when_all_present(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.diffcomp_bin)
        dirs = _make_campaign_dirs(tmp_path)
        queue_dir = dirs["afl_output"] / "default" / "queue"
        queue_dir.mkdir(parents=True)
        (queue_dir / "id:000000").write_text("val x = 1")

        stage = DiffcompStage()
        # Should not raise
        stage.validate(tmp_path, config)

    def test_execute_copies_queue_files_with_sml_extension(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.diffcomp_bin)
        dirs = _make_campaign_dirs(tmp_path)
        queue_dir = dirs["afl_output"] / "default" / "queue"
        queue_dir.mkdir(parents=True)
        original_content = "val x = 1"
        (queue_dir / "id:000000,src:000000").write_text(original_content)

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="diffcomp",
            exit_code=0,
            duration_seconds=1.0,
            stdout="",
            stderr="",
            output_dir=dirs["diffcomp_output"],
        )

        stage = DiffcompStage()
        stage.execute(tmp_path, config, runner)

        staged_file = dirs["diffcomp_input"] / "id:000000,src:000000.sml"
        assert staged_file.exists()
        assert staged_file.read_text() == original_content

    def test_execute_calls_runner_with_correct_diffcomp_command(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.diffcomp_bin)
        dirs = _make_campaign_dirs(tmp_path)
        queue_dir = dirs["afl_output"] / "default" / "queue"
        queue_dir.mkdir(parents=True)
        (queue_dir / "id:000000").write_text("val x = 1")

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="diffcomp",
            exit_code=0,
            duration_seconds=1.0,
            stdout="",
            stderr="",
            output_dir=dirs["diffcomp_output"],
        )

        stage = DiffcompStage()
        stage.execute(tmp_path, config, runner)

        runner.run.assert_called_once()
        call_args = runner.run.call_args
        cmd = call_args[1]["cmd"] if "cmd" in call_args[1] else call_args[0][0]

        # First element is the resolved diffcomp binary path
        assert str(config.diffcomp_bin.resolve()) == cmd[0]
        # Contains --output-dir and the resolved diffcomp_output path
        assert "--output-dir" in cmd
        output_idx = cmd.index("--output-dir")
        assert str((tmp_path / "diffcomp_output").resolve()) == cmd[output_idx + 1]
        # Contains --polylex and the resolved polylex_bin path
        assert "--polylex" in cmd
        polylex_idx = cmd.index("--polylex")
        assert str(config.polylex_bin.resolve()) == cmd[polylex_idx + 1]
        # Stage name is diffcomp
        stage_name = call_args[1].get("stage_name", call_args[0][1] if len(call_args[0]) > 1 else None)
        assert stage_name == "diffcomp"

    def test_execute_skips_dotfiles_and_readme(self, tmp_path: Path):
        from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage

        config = _make_config(tmp_path)
        _make_fake_binary(config.diffcomp_bin)
        dirs = _make_campaign_dirs(tmp_path)
        queue_dir = dirs["afl_output"] / "default" / "queue"
        queue_dir.mkdir(parents=True)
        (queue_dir / ".state").write_text("internal")
        (queue_dir / "README.txt").write_text("AFL++ readme")
        (queue_dir / "id:000000").write_text("val x = 1")

        runner = MagicMock(spec=ProcessRunner)
        runner.run.return_value = StageResult(
            stage_name="diffcomp",
            exit_code=0,
            duration_seconds=1.0,
            stdout="",
            stderr="",
            output_dir=dirs["diffcomp_output"],
        )

        stage = DiffcompStage()
        stage.execute(tmp_path, config, runner)

        # Only id:000000.sml should be staged, not .state.sml or README.txt.sml
        staged_files = list(dirs["diffcomp_input"].glob("*.sml"))
        assert len(staged_files) == 1
        assert staged_files[0].name == "id:000000.sml"
