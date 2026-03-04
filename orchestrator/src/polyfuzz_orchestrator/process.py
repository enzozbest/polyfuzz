"""ProcessRunner subprocess wrapper with timing and error capture."""

from __future__ import annotations

import os
import shutil
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from polyfuzz_orchestrator.config import PipelineConfig


@dataclass(frozen=True)
class StageResult:
    """Result of executing a pipeline stage."""

    stage_name: str
    exit_code: int
    duration_seconds: float
    stdout: str
    stderr: str
    output_dir: Path


class ProcessRunner:
    """Wraps subprocess.run with timing, output capture, and timeout handling."""

    def run(
        self,
        cmd: list[str],
        stage_name: str,
        output_dir: Path,
        timeout_s: int,
        env: dict[str, str] | None = None,
    ) -> StageResult:
        """Run a subprocess and capture its output.

        On timeout, returns StageResult with exit_code=-1 and stderr containing 'TIMEOUT after {timeout_s}s'.
        """
        start = time.monotonic()
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=timeout_s,
                env=env,
            )
        except subprocess.TimeoutExpired as e:
            duration = time.monotonic() - start
            return StageResult(
                stage_name=stage_name,
                exit_code=-1,
                duration_seconds=duration,
                stdout=e.stdout or "",
                stderr=f"TIMEOUT after {timeout_s}s: {e.stderr or ''}",
                output_dir=output_dir,
            )

        duration = time.monotonic() - start
        return StageResult(
            stage_name=stage_name,
            exit_code=result.returncode,
            duration_seconds=duration,
            stdout=result.stdout,
            stderr=result.stderr,
            output_dir=output_dir,
        )


def verify_components(config: PipelineConfig) -> list[str]:
    """Check all pipeline components exist and are executable.

    For each component: checks Path.exists() and os.access(X_OK).
    Also checks that 'java' is available via shutil.which (required for diffcomp).

    Returns list of error strings (empty = all good).
    """
    errors: list[str] = []

    bin_checks = [
        (config.smlgen_bin, "smlgen binary"),
        (config.polylex_bin, "polylex binary"),
        (config.afl_fuzz_bin, "afl-fuzz binary"),
        (config.diffcomp_bin, "diffcomp binary"),
    ]
    for path, label in bin_checks:
        if not path.exists():
            errors.append(f"{label} not found at {path}")
        elif not os.access(path, os.X_OK):
            errors.append(f"{label} at {path} is not executable")

    if shutil.which("java") is None:
        errors.append("java not found on PATH (required for diffcomp)")

    return errors
