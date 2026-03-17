from pathlib import Path

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PreflightError
from polyfuzz_orchestrator.process import ProcessRunner, StageResult
from polyfuzz_orchestrator.stages.validation import validate_single
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
        queue_dir = self._find_queue_dir(campaign_dir)
        diffcomp_errors = validate_single(config.diffcomp_bin, "diffcomp")
        queue_dir_errors = (
            []
            if queue_dir
            else [f"AFL++ queue directory not found under {campaign_dir / 'afl_output'}"]
        )

        file_errors = []
        if not queue_dir_errors:
            input_files = self._list_input_files(queue_dir)
            file_errors = [] if input_files else [f"AFL++ queue directory at {queue_dir} is empty"]

        errors = [*diffcomp_errors, *queue_dir_errors, *file_errors]
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
        queue_dir = self._find_queue_dir(campaign_dir)
        staging_dir = campaign_dir / "diffcomp_input"
        output_dir = campaign_dir / "diffcomp_output"

        # Ensure directories exist
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
    def _find_queue_dir(campaign_dir: Path) -> Path | None:
        """Locate the AFL++ queue directory under afl_output.

        AFL++ names its output subdirectory after the fuzzer instance — ``default`` in
        single-fuzzer mode, or the name passed via ``-M``/``-S`` in parallel mode.
        This method scans for the first subdirectory containing a ``queue/`` folder.
        """
        afl_output = campaign_dir / "afl_output"
        if not afl_output.exists():
            return None
        for child in sorted(afl_output.iterdir()):
            queue = child / "queue"
            if queue.is_dir():
                return queue
        return None

    @staticmethod
    def _list_input_files(queue_dir: Path) -> list[Path]:
        """List input files in AFL++ queue dir, excluding dotfiles and README.txt."""
        return sorted(
            f
            for f in queue_dir.iterdir()
            if f.is_file() and not f.name.startswith(".") and f.name != "README.txt"
        )
