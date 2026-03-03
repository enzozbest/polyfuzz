from __future__ import annotations

import os
from pathlib import Path

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PreflightError
from polyfuzz_orchestrator.process import ProcessRunner, StageResult
from polyfuzz_orchestrator.stages.base import Stage


class DiffcompStage(Stage):
    """Invoke the diffcomp tool to perform differential comparison of token streams.

    Copies AFL++ queue files to a staging directory with ``.sml`` extensions (required by diffcomp's FileDiscovery),
    then invokes diffcomp.
    """

    @property
    def name(self) -> str:
        return "diffcomp"

    def validate(self, campaign_dir: Path, config: PipelineConfig) -> None:
        """Verify diffcomp is executable and AFL++ queue has files."""
        errors: list[str] = []

        if not config.diffcomp_bin.exists():
            errors.append(f"diffcomp not found at {config.diffcomp_bin}")
        elif not os.access(config.diffcomp_bin, os.X_OK):
            errors.append(
                f"diffcomp at {config.diffcomp_bin} is not executable"
            )

        queue_dir = campaign_dir / "afl_output" / "default" / "queue"
        if not queue_dir.exists():
            errors.append(f"AFL++ queue directory not found at {queue_dir}")
        else:
            input_files = self._list_input_files(queue_dir)
            if not input_files:
                errors.append(f"AFL++ queue directory at {queue_dir} is empty")

        if errors:
            raise PreflightError(errors)

    def execute(
        self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
    ) -> StageResult:
        """Invoke diffcomp on AFL++ queue files.

        1. Copy queue files to staging directory with .sml extension.
        2. Build diffcomp command with absolute paths.
        3. Run and return result.
        """
        queue_dir = campaign_dir / "afl_output" / "default" / "queue"
        staging_dir = campaign_dir / "diffcomp_input"
        output_dir = campaign_dir / "diffcomp_output"

        #Ensure directories exist
        staging_dir.mkdir(parents=True, exist_ok=True)
        output_dir.mkdir(parents=True, exist_ok=True)

        # Copy queue files with .sml extension for diffcomp FileDiscovery
        input_files = self._list_input_files(queue_dir)
        for f in input_files:
            (staging_dir / f"{f.name}.sml").write_bytes(f.read_bytes())

        cmd = [
            str(config.diffcomp_bin.resolve()),
            str(staging_dir.resolve()),
            "--output-dir",
            str(output_dir.resolve()),
            "--polylex",
            str(config.polylex_bin.resolve()),
        ]

        return runner.run(
            cmd=cmd,
            stage_name=self.name,
            output_dir=output_dir,
            timeout_s=config.stage_timeout_s,
        )

    @staticmethod
    def _list_input_files(queue_dir: Path) -> list[Path]:
        """List input files in AFL++ queue dir, excluding dotfiles and README.txt."""
        return sorted(
            f
            for f in queue_dir.iterdir()
            if f.is_file() and not f.name.startswith(".") and f.name != "README.txt"
        )
