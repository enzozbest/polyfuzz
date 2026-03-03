from __future__ import annotations

from pathlib import Path


def create_campaign_layout(work_dir: Path) -> dict[str, Path]:
    """Create the directory structure for a single campaign.

    Creates and returns paths for: corpus, afl_output, diffcomp_input,
    diffcomp_output, results.
    """
    dirs = {
        "corpus": work_dir / "corpus",
        "afl_output": work_dir / "afl_output",
        "diffcomp_input": work_dir / "diffcomp_input",
        "diffcomp_output": work_dir / "diffcomp_output",
        "results": work_dir / "results",
    }
    for d in dirs.values():
        d.mkdir(parents=True, exist_ok=True)
    return dirs


def create_experiment_layout(work_dir: Path, num_campaigns: int) -> list[Path]:
    """Create the top-level experiment directory layout with campaign directories.

    Creates campaign directories named campaign_000/, campaign_001/, etc.
    Args:
        work_dir: Experiment working directory.
        num_campaigns: Number of campaign directories to create.
    Returns:
        List of campaign directory Paths, in order.
    """
    campaign_dirs: list[Path] = []
    for i in range(num_campaigns):
        campaign_dir = work_dir / f"campaign_{i:03d}"
        campaign_dir.mkdir(parents=True, exist_ok=True)
        campaign_dirs.append(campaign_dir)
    return campaign_dirs
