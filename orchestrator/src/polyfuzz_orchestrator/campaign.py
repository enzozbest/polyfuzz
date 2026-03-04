from __future__ import annotations

import shutil
from datetime import UTC, datetime
from pathlib import Path

from rich.console import Console

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.layout import create_experiment_layout
from polyfuzz_orchestrator.manifest import (
    build_campaign_manifest,
    collect_metadata,
    is_campaign_complete,
    update_experiment_manifest,
    write_experiment_manifest,
    write_manifest,
)
from polyfuzz_orchestrator.pipeline import PipelineExecutor
from polyfuzz_orchestrator.seed import derive_campaign_seed, generate_master_seed


class CampaignOrchestrator:
    """Runs N campaigns in batch with isolation, seeding, and resumability.

    Each campaign runs in its own ``campaign_NNN/`` subdirectory with a deterministic seed derived from the
    experiment-level master seed. Completed campaigns are detected and skipped on re-run. The master seed is persisted
    to the experiment manifest before any campaign starts so it survives crashes.
    """

    def __init__(self, config: PipelineConfig) -> None:
        self._config = config
        self._console = Console()

    def run(self, *, no_analytics: bool = False) -> list[Path]:
        """Execute all campaigns and return a list of completed campaign directories.

        Args:
            no_analytics: If True, skip automatic analytics after campaigns complete.

        Raises:
            PreflightError: If component verification fails in any campaign.
            PipelineError: If any pipeline stage fails. The failing campaign's
                manifest is NOT written, so it will be re-run on the next invocation.
        """

        master_seed = self._initialize_experiment()
        metadata = collect_metadata(self._config)

        completed: list[Path] = []
        for i in range(self._config.num_campaigns):
            campaign_dir = self._run_campaign(i, master_seed, metadata)
            completed.append(campaign_dir)

        self._console.print(
            f"\n[bold green]All campaigns complete: "
            f"{len(completed)}/{self._config.num_campaigns}[/bold green]"
        )

        if not no_analytics:
            self._run_post_campaign_analytics()

        return completed

    def _initialize_experiment(self) -> int:
        """Set up the experiment layout and master seed."""
        master_seed = self._config.seed if self._config.seed is not None else generate_master_seed()
        create_experiment_layout(self._config.work_dir, self._config.num_campaigns)
        write_experiment_manifest(
            self._config.work_dir, master_seed, self._config.num_campaigns, self._config
        )
        self._console.print(f"[bold]Master seed: {master_seed}[/bold]")
        return master_seed

    def _run_campaign(self, index: int, master_seed: int, metadata: dict) -> Path:
        """Run a single campaign and return its directory."""
        campaign_dir = self._config.work_dir / f"campaign_{index:03d}"
        campaign_seed = derive_campaign_seed(master_seed, index)

        self._console.print(
            f"\n[bold blue]Campaign {index + 1}/{self._config.num_campaigns}[/bold blue]"
        )

        if is_campaign_complete(campaign_dir):
            self._console.print("[dim]  Already complete, skipping[/dim]")
            update_experiment_manifest(self._config.work_dir, index, campaign_seed, "complete")
            return campaign_dir

        if campaign_dir.exists():
            shutil.rmtree(campaign_dir)

        campaign_dir.mkdir(parents=True, exist_ok=True)
        campaign_config = PipelineConfig(
            work_dir=campaign_dir,
            tests_per_campaign=self._config.tests_per_campaign,
            seed=campaign_seed,
            num_campaigns=1,
            afl_timeout_s=self._config.afl_timeout_s,
            afl_exec_timeout_ms=self._config.afl_exec_timeout_ms,
            stage_timeout_s=self._config.stage_timeout_s,
            smlgen_bin=self._config.smlgen_bin,
            polylex_bin=self._config.polylex_bin,
            diffcomp_bin=self._config.diffcomp_bin,
            afl_fuzz_bin=self._config.afl_fuzz_bin,
        )

        start_time = datetime.now(UTC).isoformat()
        try:
            executor = PipelineExecutor(campaign_config)
            results = executor.run()
        except Exception:
            self._console.print(
                f"[bold red]Campaign {index + 1}/{self._config.num_campaigns} failed[/bold red]"
            )
            raise
        end_time = datetime.now(UTC).isoformat()

        manifest_data = build_campaign_manifest(
            index, master_seed, campaign_seed, self._config, results, metadata,
            start_time, end_time,
        )
        write_manifest(campaign_dir, manifest_data)
        update_experiment_manifest(self._config.work_dir, index, campaign_seed, "complete")

        return campaign_dir

    def _run_post_campaign_analytics(self) -> None:
        """Run analytics on the completed campaigns."""
        try:
            from polyfuzz_orchestrator.analytics import run_analytics

            self._console.print("\n[bold]Running analytics...[/bold]")
            run_analytics(self._config.work_dir)
        except Exception as e:
            self._console.print(
                f"[bold yellow]Analytics warning:[/bold yellow] {e}\n"
                "Campaign data is safe. Re-run analytics with: "
                "polyfuzz analyse --work-dir <dir>"
            )
