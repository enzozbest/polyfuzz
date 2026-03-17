from __future__ import annotations

from pathlib import Path

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PreflightError
from polyfuzz_orchestrator.process import ProcessRunner, StageResult
from polyfuzz_orchestrator.stages import validate_single
from polyfuzz_orchestrator.stages.base import Stage


class SmlgenStage(Stage):
    """Generate SML corpus files by invoking the smlgen wrapper script.

    smlgen is a Kotlin program built via Gradle ``installDist``.  The stage invokes the generated
    wrapper script directly with CLI arguments for count (``-n``), seed (``-seed``), and output
    directory (``-o``).
    """

    @property
    def name(self) -> str:
        return "smlgen"

    def validate(self, campaign_dir: Path, config: PipelineConfig) -> None:
        """Verify smlgen binary exists and is executable."""
        errors = validate_single(config.smlgen_bin, "smlgen")
        if errors:
            raise PreflightError(errors)

    def execute(
        self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
    ) -> StageResult:
        """Invoke smlgen to generate corpus files.

        Runs: <smlgen_bin> -n <tests_per_campaign> -o <corpus_dir> [-seed <seed>]

        Output goes to the corpus directory within campaign_dir.
        """
        corpus_dir = campaign_dir / "corpus"
        corpus_dir.mkdir(parents=True, exist_ok=True)

        cmd = [
            str(config.smlgen_bin),
            "-n",
            str(config.tests_per_campaign),
            "-o",
            str(corpus_dir),
        ]

        if config.seed is not None:
            cmd.extend(["-seed", str(config.seed)])

        return runner.run(
            cmd=cmd,
            stage_name=self.name,
            output_dir=corpus_dir,
            timeout_s=config.stage_timeout_s,
        )
