from __future__ import annotations

import os
from pathlib import Path

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PreflightError
from polyfuzz_orchestrator.process import ProcessRunner, StageResult
from polyfuzz_orchestrator.stages.validation import validate_path, validate_single, validate_sml_files_exist
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
        corpus_dir = campaign_dir / "corpus"

        afl_errors = validate_single(config.afl_fuzz_bin, "afl-fuzz")
        polylex_errors = validate_single(config.polylex_bin, "polylex")
        corpus_errors = [
            x
            for x in [
                validate_path(corpus_dir, "corpus"),
                validate_sml_files_exist(corpus_dir, "corpus"),
            ]
            if x is not None
        ]  # Trick using comprehensions for null check!

        errors = [*afl_errors, *polylex_errors, *corpus_errors]

        if errors:
            raise PreflightError(errors)

    def execute(
        self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
    ) -> StageResult:
        """Invoke afl-fuzz with correct flags and environment.

        Command: afl-fuzz -i <corpus> -o <afl_output> -V <timeout> [options] -- <polylex>
        Environment: AFL_SKIP_CPUFREQ=1, AFL_I_DONT_CARE_ABOUT_MISSING_CRASHES=1, AFL_NO_UI=1
        """
        corpus_dir = campaign_dir / "corpus"
        afl_output_dir = campaign_dir / "afl_output"
        afl_output_dir.mkdir(parents=True, exist_ok=True)

        cmd: list[str] = [
            str(config.afl_fuzz_bin),
            "-i",
            str(corpus_dir),
            "-o",
            str(afl_output_dir),
            "-V",
            str(config.afl_timeout_s),
        ]

        # Pass seed for reproducible fuzzing
        if config.seed is not None:
            cmd.extend(["-s", str(config.seed)])

        # Add per-input timeout only if explicitly configured
        if config.afl_exec_timeout_ms is not None:
            cmd.extend(["-t", str(config.afl_exec_timeout_ms)])

        cmd.extend(["--", str(config.polylex_bin)])

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
