"""Tests for ProcessRunner and verify_components."""

import stat
from pathlib import Path

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.process import ProcessRunner, StageResult, verify_components


class TestProcessRunner:
    """Test ProcessRunner subprocess wrapper."""

    def test_run_captures_stdout_stderr_exitcode_duration(self, tmp_path: Path) -> None:
        """ProcessRunner.run captures stdout, stderr, exit_code, and duration_seconds from a subprocess."""
        runner = ProcessRunner()
        result = runner.run(
            cmd=["python3", "-c", "import sys; print('hello'); print('err', file=sys.stderr)"],
            stage_name="test_stage",
            output_dir=tmp_path,
            timeout_s=10,
        )

        assert isinstance(result, StageResult)
        assert result.stage_name == "test_stage"
        assert result.exit_code == 0
        assert "hello" in result.stdout
        assert "err" in result.stderr
        assert result.duration_seconds > 0
        assert result.output_dir == tmp_path

    def test_run_returns_timeout_result(self, tmp_path: Path) -> None:
        """ProcessRunner.run returns exit_code=-1 and stderr containing 'TIMEOUT' when subprocess times out."""
        runner = ProcessRunner()
        result = runner.run(
            cmd=["python3", "-c", "import time; time.sleep(30)"],
            stage_name="slow_stage",
            output_dir=tmp_path,
            timeout_s=1,
        )

        assert result.exit_code == -1
        assert "TIMEOUT" in result.stderr

    def test_run_captures_nonzero_exit_code(self, tmp_path: Path) -> None:
        """ProcessRunner.run captures nonzero exit codes."""
        runner = ProcessRunner()
        result = runner.run(
            cmd=["python3", "-c", "import sys; sys.exit(42)"],
            stage_name="failing_stage",
            output_dir=tmp_path,
            timeout_s=10,
        )

        assert result.exit_code == 42


class TestVerifyComponents:
    """Test verify_components pre-flight check."""

    def test_returns_empty_list_when_all_exist(self, tmp_path: Path) -> None:
        """verify_components returns empty list when all component paths exist and are accessible."""
        # Create fake component files
        smlgen_bin = tmp_path / "smlgen_bin"
        smlgen_bin.touch()
        smlgen_bin.chmod(smlgen_bin.stat().st_mode | stat.S_IEXEC)

        polylex_bin = tmp_path / "polylex_fuzz"
        polylex_bin.touch()
        polylex_bin.chmod(polylex_bin.stat().st_mode | stat.S_IEXEC)

        diffcomp_bin = tmp_path / "diffcomp"
        diffcomp_bin.touch()
        diffcomp_bin.chmod(diffcomp_bin.stat().st_mode | stat.S_IEXEC)

        afl_bin = tmp_path / "afl-fuzz"
        afl_bin.touch()
        afl_bin.chmod(afl_bin.stat().st_mode | stat.S_IEXEC)

        config = PipelineConfig(
            work_dir=tmp_path,
            smlgen_bin=smlgen_bin,
            polylex_bin=polylex_bin,
            diffcomp_bin=diffcomp_bin,
            afl_fuzz_bin=afl_bin,
        )

        errors = verify_components(config)
        assert errors == []

    def test_returns_errors_for_missing_components(self, tmp_path: Path) -> None:
        """verify_components returns error strings for each missing component."""
        config = PipelineConfig(
            work_dir=tmp_path,
            smlgen_bin=tmp_path / "nonexistent_smlgen",
            polylex_bin=tmp_path / "nonexistent_bin",
            diffcomp_bin=tmp_path / "nonexistent_diffcomp",
            afl_fuzz_bin=tmp_path / "nonexistent_afl",
        )

        errors = verify_components(config)
        assert len(errors) >= 4  # At least one per missing component
        # Each error should mention the component that's missing
        combined = " ".join(errors)
        assert "smlgen" in combined.lower() or "nonexistent_smlgen" in combined
        assert "polylex" in combined.lower() or "nonexistent_bin" in combined
        assert "diffcomp" in combined.lower() or "nonexistent_diffcomp" in combined
        assert "afl" in combined.lower() or "nonexistent_afl" in combined
