from __future__ import annotations

import os
from pathlib import Path

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PreflightError
from polyfuzz_orchestrator.process import ProcessRunner, StageResult
from polyfuzz_orchestrator.stages.base import Stage


class AflStage(Stage):
    """Run an AFL++ fuzzing campaign against the instrumented polylex harness.

    AFL++ reads seed corpus from the corpus directory, fuzzes the instrumented polylex binary, and writes findings
    to the afl_output directory. The stage sets required AFL++ environment variables for headless operation.
    """

    @property
    def name(self) -> str:
        return "afl"

    def validate(self, campaign_dir: Path, config: PipelineConfig) -> None:
        """Verify afl-fuzz exists, polylex binary exists, and corpus is non-empty."""
        errors: list[str] = []

        if not config.afl_fuzz_bin.exists():
            errors.append(f"afl-fuzz not found at {config.afl_fuzz_bin}")
        elif not os.access(config.afl_fuzz_bin, os.X_OK):
            errors.append(f"afl-fuzz at {config.afl_fuzz_bin} is not executable")

        if not config.polylex_bin.exists():
            errors.append(f"polylex binary not found at {config.polylex_bin}")
        elif not os.access(config.polylex_bin, os.X_OK):
            errors.append(f"polylex binary at {config.polylex_bin} is not executable")

        corpus_dir = campaign_dir / "corpus"
        if not corpus_dir.exists():
            os.mkdir(corpus_dir)
            errors.append(f"corpus directory not found at {corpus_dir}. Did smlgen run?")
        else:
            sml_files = list(corpus_dir.glob("*.sml"))
            all_files = list(corpus_dir.iterdir())
            if not sml_files and not all_files:
                errors.append(f"corpus directory at {corpus_dir} is empty")

        if errors:
            raise PreflightError(errors)

    def execute(
        self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
    ) -> StageResult:
        """Invoke afl-fuzz with correct flags and environment.

        Command: afl-fuzz -i <corpus> -o <afl_output> -V <timeout> [options] -- <polylex> @@
        Environment: AFL_SKIP_CPUFREQ=1, AFL_I_DONT_CARE_ABOUT_MISSING_CRASHES=1, AFL_NO_UI=1
        """
        corpus_dir = campaign_dir / "corpus"
        afl_output_dir = campaign_dir / "afl_output"
        afl_output_dir.mkdir(parents=True, exist_ok=True)

        cmd: list[str] = [
            str(config.afl_fuzz_bin),
            "-i", str(corpus_dir),
            "-o", str(afl_output_dir),
            "-V", str(config.afl_timeout_s),
        ]

        # Pass seed for reproducible fuzzing
        if config.seed is not None:
            cmd.extend(["-s", str(config.seed)])

        # Add per-input timeout only if explicitly configured
        if config.afl_exec_timeout_ms is not None:
            cmd.extend(["-t", str(config.afl_exec_timeout_ms)])

        cmd.extend(["--", str(config.polylex_bin), "@@"])

        # Merge with current environment and add AFL++ headless variables
        env = {
            **os.environ,
            "AFL_SKIP_CPUFREQ": "1",
            "AFL_I_DONT_CARE_ABOUT_MISSING_CRASHES": "1",
            "AFL_NO_UI": "1",
        }

        return runner.run(
            cmd=cmd,
            stage_name=self.name,
            output_dir=afl_output_dir,
            timeout_s=config.stage_timeout_s,
            env=env,
        )
