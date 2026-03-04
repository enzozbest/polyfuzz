from __future__ import annotations

from pathlib import Path

import click
from rich.console import Console

from datetime import UTC, datetime
from polyfuzz_orchestrator.campaign import CampaignOrchestrator
from polyfuzz_orchestrator.config import PipelineConfig, load_config
from polyfuzz_orchestrator.errors import PipelineError, PreflightError
from polyfuzz_orchestrator.manifest import (
    build_campaign_manifest,
    collect_metadata,
    write_manifest,
)
from polyfuzz_orchestrator.pipeline import PipelineExecutor
from polyfuzz_orchestrator.seed import generate_master_seed

console = Console()


@click.group(invoke_without_command=True)
@click.option(
    "--work-dir",
    "-d",
    type=click.Path(exists=True, path_type=Path),
    required=True,
    help="Working directory for campaign output.",
)
@click.option(
    "--tests",
    "-n",
    type=int,
    default=100,
    show_default=True,
    help="Number of tests per campaign.",
)
@click.option(
    "--seed",
    "-s",
    type=int,
    default=None,
    help="Seed (omit for random).",
)
@click.option(
    "--campaigns",
    "-N",
    type=int,
    default=1,
    show_default=True,
    help="Number of campaigns to run.",
)
@click.option(
    "--afl-timeout",
    type=int,
    default=300,
    show_default=True,
    help="AFL++ campaign duration in seconds.",
)
@click.option(
    "--config",
    "-c",
    type=click.Path(exists=True, path_type=Path),
    default=None,
    help="TOML configuration file.",
)
@click.option(
    "--no-analytics",
    is_flag=True,
    default=False,
    help="Skip automatic analytics after campaign completion.",
)
@click.pass_context
def cli(
    ctx: click.Context,
    work_dir: Path,
    tests: int,
    seed: int | None,
    campaigns: int,
    afl_timeout: int,
    config: Path | None,
    no_analytics: bool,
) -> None:
    """PolyFuzz differential testing orchestrator."""
    ctx.ensure_object(dict)
    cfg = load_config(
        toml_path=config,
        work_dir=work_dir,
        tests_per_campaign=tests,
        seed=seed,
        num_campaigns=campaigns,
        afl_timeout_s=afl_timeout,
    )
    ctx.obj["config"] = cfg

    if ctx.invoked_subcommand is None:
        if cfg.num_campaigns > 1:
            orchestrator = CampaignOrchestrator(cfg)
            try:
                orchestrator.run(no_analytics=no_analytics)
            except PreflightError as e:
                console.print(f"[bold red]Pre-flight check failed:[/bold red]\n{e}")
                raise SystemExit(1)
            except PipelineError as e:
                console.print(
                    f"[bold red]Pipeline failed at stage '{e.stage_name}'[/bold red]"
                )
                raise SystemExit(e.exit_code)
        else:
            # Single-campaign mode: we want to ensure isolation in campaign_000/
            # just like the CampaignOrchestrator does.
            master_seed = cfg.seed if cfg.seed is not None else str(generate_master_seed())
            campaign_dir = cfg.work_dir / "campaign_000"
            campaign_dir.mkdir(parents=True, exist_ok=True)
            
            # Re-configure to point to the campaign-specific directory
            campaign_cfg = PipelineConfig(
                work_dir=campaign_dir,
                tests_per_campaign=cfg.tests_per_campaign,
                seed=int(master_seed),
                num_campaigns=1,
                afl_timeout_s=cfg.afl_timeout_s,
                afl_exec_timeout_ms=cfg.afl_exec_timeout_ms,
                stage_timeout_s=cfg.stage_timeout_s,
                smlgen_jar=cfg.smlgen_jar,
                polylex_bin=cfg.polylex_bin,
                diffcomp_bin=cfg.diffcomp_bin,
                afl_fuzz_bin=cfg.afl_fuzz_bin,
            )
            
            executor = PipelineExecutor(campaign_cfg)
            try:
                metadata = collect_metadata(cfg)
                start_time = datetime.now(UTC).isoformat()
                results = executor.run()
                end_time = datetime.now(UTC).isoformat()
                
                manifest_data = build_campaign_manifest(
                    campaign_index=0,
                    master_seed=int(master_seed),
                    campaign_seed=int(master_seed),
                    config=cfg,
                    results=results,
                    metadata=metadata,
                    start_time=start_time,
                    end_time=end_time,
                )
                write_manifest(campaign_dir, manifest_data)
            except PreflightError as e:
                console.print(f"[bold red]Pre-flight check failed:[/bold red]\n{e}")
                raise SystemExit(1)
            except PipelineError as e:
                console.print(
                    f"[bold red]Pipeline failed at stage '{e.stage_name}'[/bold red]"
                )
                raise SystemExit(e.exit_code)


@cli.command("only-smlgen")
@click.pass_context
def only_smlgen(ctx: click.Context) -> None:
    """Run only the smlgen corpus generation stage."""
    executor = PipelineExecutor(ctx.obj["config"])
    try:
        executor.run(only_stage="smlgen")
    except (PreflightError, PipelineError) as e:
        console.print(f"[bold red]{e}[/bold red]")
        raise SystemExit(1)


@cli.command("only-afl")
@click.pass_context
def only_afl(ctx: click.Context) -> None:
    """Run only the AFL++ fuzzing stage."""
    executor = PipelineExecutor(ctx.obj["config"])
    try:
        executor.run(only_stage="afl")
    except (PreflightError, PipelineError) as e:
        console.print(f"[bold red]{e}[/bold red]")
        raise SystemExit(1)


@cli.command("only-diffcomp")
@click.pass_context
def only_diffcomp(ctx: click.Context) -> None:
    """Run only the diffcomp differential comparison stage."""
    executor = PipelineExecutor(ctx.obj["config"])
    try:
        executor.run(only_stage="diffcomp")
    except (PreflightError, PipelineError) as e:
        console.print(f"[bold red]{e}[/bold red]")
        raise SystemExit(1)


@cli.command("analyse")
@click.pass_context
def analyse(ctx: click.Context) -> None:
    """Run analytics on existing experiment output."""
    from polyfuzz_orchestrator.analytics import run_analytics

    cfg = ctx.obj["config"]
    try:
        run_analytics(cfg.work_dir)
    except ValueError as e:
        console.print(f"[bold red]Analytics error:[/bold red] {e}")
        raise SystemExit(1)
    except Exception as e:
        console.print(f"[bold red]Analytics failed:[/bold red] {e}")
        raise SystemExit(1)


def main() -> None:
    """Entry point referenced in pyproject.toml."""
    cli()
