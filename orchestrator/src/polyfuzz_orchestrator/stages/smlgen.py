from __future__ import annotations

import shutil
from pathlib import Path

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PreflightError
from polyfuzz_orchestrator.process import ProcessRunner, StageResult
from polyfuzz_orchestrator.stages.base import Stage


class SmlgenStage(Stage):
    """Generate SML corpus files by invoking smlgen via java -cp.

    smlgen is a Kotlin program packaged as a JAR. The stage invokes ``java -cp <jar> bestetti.enzo.smlgen.MainKt``
    with CLI arguments for count (``-n``), seed (``-seed``), and output directory (``-o``).
    """

    @property
    def name(self) -> str:
        return "smlgen"

    def validate(self, campaign_dir: Path, config: PipelineConfig) -> None:
        """Verify smlgen JAR exists and java is available on PATH."""
        errors: list[str] = []

        if not config.smlgen_jar.exists():
            errors.append(f"smlgen JAR not found at {config.smlgen_jar}")

        if shutil.which("java") is None:
            errors.append("java not found on PATH (required for smlgen)")

        if errors:
            raise PreflightError(errors)

    def execute(
        self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
    ) -> StageResult:
        """Invoke smlgen to generate corpus files.

        Runs: java -cp <jar> bestetti.enzo.smlgen.MainKt
              -n <tests_per_campaign> -o <corpus_dir> [-seed <seed>]

        Output goes to the corpus directory within campaign_dir.
        """
        corpus_dir = campaign_dir / "corpus"
        corpus_dir.mkdir(parents=True, exist_ok=True)

        cmd = [
            f"{str(config.smlgen_jar)}",
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
