from __future__ import annotations

from pathlib import Path

import click
from rich.console import Console

from polyfuzz_orchestrator.campaign import CampaignOrchestrator
from polyfuzz_orchestrator.config import load_config
from polyfuzz_orchestrator.errors import PipelineError, PreflightError
from polyfuzz_orchestrator.pipeline import PipelineExecutor

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
