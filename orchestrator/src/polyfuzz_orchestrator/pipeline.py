from __future__ import annotations

from rich.console import Console

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PipelineError, PreflightError
from polyfuzz_orchestrator.layout import create_campaign_layout, create_experiment_layout
from polyfuzz_orchestrator.process import ProcessRunner, StageResult, verify_components
from polyfuzz_orchestrator.stages import (
    AflStage,
    DiffcompStage,
    SmlgenStage,
)
from polyfuzz_orchestrator.stages.base import Stage

console = Console()


class PipelineExecutor:
    """Sequences pipeline stages in correct order with error propagation.

    Runs pre-flight verification, creates campaign directory layout, then executes stages in order:
    smlgen -> afl -> diffcomp.
    Stops on first stage failure and raises PipelineError with stage details, or when all stages complete successfully.
    """

    STAGE_ORDER = ["smlgen", "afl", "diffcomp"]

    def __init__(self, config: PipelineConfig) -> None:
        self._config = config
        self._runner = ProcessRunner()
        self._stages: dict[str, Stage] = {
            "smlgen": SmlgenStage(),
            "afl": AflStage(),
            "diffcomp": DiffcompStage(),
        }

    def run(self, only_stage: str | None = None) -> list[StageResult]:
        """Execute the pipeline, optionally running only a single stage.
        Args:
            only_stage: If set, run only this stage. Otherwise run all stages
                in STAGE_ORDER.
        Returns:
            List of StageResult for all completed stages.
        Raises:
            PreflightError: If verify_components finds missing components.
            PipelineError: If any stage returns a non-zero exit code.
        """

        errors = verify_components(self._config)
        if errors:
            raise PreflightError(errors)

        create_campaign_layout(self._config.work_dir)

        stage_names = [only_stage] if only_stage else self.STAGE_ORDER

        results: list[StageResult] = []
        for stage_name in stage_names:
            stage = self._stages[stage_name]
            console.print(f"[bold blue]>>> Stage: {stage.name}[/bold blue]")
            stage.validate(self._config.work_dir, self._config)
            result = stage.execute(self._config.work_dir, self._config, self._runner)
            if result.exit_code != 0:
                console.print(
                    f"[bold red]Stage '{stage.name}' failed "
                    f"(exit code {result.exit_code})[/bold red]"
                )
                if result.stderr:
                    console.print(f"[red]  stderr: {result.stderr}[/red]")
                raise PipelineError(
                    stage_name=stage.name,
                    exit_code=result.exit_code,
                    stderr=result.stderr,
                    stdout=result.stdout,
                )

            console.print(
                f"[green]  completed in {result.duration_seconds:.1f}s[/green]"
            )
            results.append(result)

        total_time = sum(r.duration_seconds for r in results)
        console.print(
            f"\n[bold green]Pipeline complete: "
            f"{len(results)} stage(s) in {total_time:.1f}s[/bold green]"
        )

        return results
