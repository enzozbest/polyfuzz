from __future__ import annotations

import json
import os
import platform
import subprocess
import sys
import tempfile
from datetime import UTC, datetime
from pathlib import Path
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from polyfuzz_orchestrator.config import PipelineConfig

MANIFEST_FILENAME = "manifest.json"
EXPERIMENT_MANIFEST_FILENAME = "experiment_manifest.json"


def is_campaign_complete(campaign_dir: Path) -> bool:
    """Check whether a campaign has completed successfully.

    Reads the campaign manifest and checks for status == "complete".
    Args:
        campaign_dir: Path to the campaign directory.
    Returns:
        True if the campaign manifest indicates completion.
        False on any error (missing file, invalid JSON, etc.).
    """
    try:
        manifest_path = campaign_dir / MANIFEST_FILENAME
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
        return data.get("status") == "complete"
    except (json.JSONDecodeError, OSError):
        return False


def write_manifest(campaign_dir: Path, manifest_data: dict) -> None:
    """Write a manifest file."""
    manifest_path = campaign_dir / MANIFEST_FILENAME
    _atomic_write_json(manifest_path, manifest_data)


def _atomic_write_json(path: Path, data: dict) -> None:
    """Write a JSON file atomically using tempfile + os.replace.

    Internal helper for writing any JSON file atomically.
    Creates a temporary file in the campaign directory, writes the JSON content, fsyncs,
    then atomically replaces the target manifest file.
    Args:
        path: Path to the target file.
        data: Dictionary to serialize as JSON.
    Raises:
        OSError: If the atomic write fails.
    """
    content = json.dumps(data, indent=2, default=str)
    fd, tmp_path = tempfile.mkstemp(
        dir=path.parent, prefix=".manifest_", suffix=".tmp"
    )
    try:
        os.write(fd, content.encode("utf-8"))
        os.fsync(fd)
        os.close(fd)
        fd = -1
        os.replace(tmp_path, path)
    except BaseException:
        if fd >= 0:
            os.close(fd)
        try:
            os.unlink(tmp_path)
        except OSError:
            pass
        raise


def collect_metadata(config: PipelineConfig) -> dict:
    """Collect system and tool version metadata for experiment provenance.

    All collection is best-effort -- missing tools or errors result in
    None values rather than exceptions.
    Args:
        config: Pipeline configuration (used to locate tool binaries).
    Returns:
        Dictionary with git_commit, tool_versions, system, and timestamp.
    """
    git_commit = _run_quiet(["git", "rev-parse", "HEAD"])
    afl_version = _run_quiet([str(config.afl_fuzz_bin), "--version"])
    java_version = _run_quiet(["java", "--version"])
    uname = platform.uname()

    return {
        "git_commit": git_commit,
        "tool_versions": {
            "python": sys.version,
            "afl_fuzz": afl_version,
            "java": java_version,
        },
        "system": {
            "platform": uname.system,
            "machine": uname.machine,
            "node": uname.node,
            "python_version": sys.version,
        },
        "timestamp": datetime.now(UTC).isoformat(),
    }


def _run_quiet(cmd: list[str]) -> str | None:
    """Run a command and return its stdout, or None on any error."""
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=10,
        )
        output = result.stdout.strip()
        if not output:
            output = result.stderr.strip()
        return output or None
    except (FileNotFoundError, subprocess.TimeoutExpired, OSError):
        return None


def build_campaign_manifest(
    campaign_index: int,
    master_seed: int,
    campaign_seed: int,
    config: PipelineConfig,
    results: list,
    metadata: dict,
    start_time: str,
    end_time: str,
) -> dict:
    """Build the full campaign manifest dictionary.

    Args:
        campaign_index: Zero-based campaign index.
        master_seed: Experiment-level master seed.
        campaign_seed: Derived seed for this campaign.
        config: Pipeline configuration.
        results: List of StageResult objects from pipeline execution.
        metadata: System metadata from collect_metadata().
        start_time: ISO 8601 campaign start timestamp.
        end_time: ISO 8601 campaign end timestamp.
    Returns:
        Complete manifest dictionary ready for write_manifest().
    """
    start_dt = datetime.fromisoformat(start_time)
    end_dt = datetime.fromisoformat(end_time)
    duration_seconds = (end_dt - start_dt).total_seconds()

    stages_timing = {}
    for r in results:
        stages_timing[r.stage_name] = r.duration_seconds

    return {
        "status": "complete",
        "campaign_index": campaign_index,
        "master_seed": master_seed,
        "campaign_seed": campaign_seed,
        "config": {
            "tests_per_campaign": config.tests_per_campaign,
            "afl_timeout_s": config.afl_timeout_s,
            "stage_timeout_s": config.stage_timeout_s,
            "afl_exec_timeout_ms": config.afl_exec_timeout_ms,
        },
        "metadata": metadata,
        "timing": {
            "start": start_time,
            "end": end_time,
            "duration_seconds": duration_seconds,
            "stages": stages_timing,
        },
    }


def write_experiment_manifest(
    work_dir: Path,
    master_seed: int,
    num_campaigns: int,
    config: PipelineConfig,
) -> None:
    """Write an experiment-level manifest atomically.

    Creates the initial experiment manifest before any campaigns run.
    Args:
        work_dir: Experiment working directory.
        master_seed: Master seed for the experiment.
        num_campaigns: Number of campaigns planned.
        config: Pipeline configuration.
    """
    manifest_path = work_dir / EXPERIMENT_MANIFEST_FILENAME
    data = {
        "master_seed": master_seed,
        "num_campaigns": num_campaigns,
        "config": {
            "tests_per_campaign": config.tests_per_campaign,
            "afl_timeout_s": config.afl_timeout_s,
            "stage_timeout_s": config.stage_timeout_s,
            "afl_exec_timeout_ms": config.afl_exec_timeout_ms,
        },
        "started_at": datetime.now(UTC).isoformat(),
        "campaigns": [],
    }
    _atomic_write_json(manifest_path, data)


def update_experiment_manifest(
    work_dir: Path,
    campaign_index: int,
    campaign_seed: int,
    status: str,
) -> None:
    """Update the experiment manifest with a campaign's status.

    Reads the existing experiment manifest, appends or updates the
    campaign entry, and writes atomically.

    Args:
        work_dir: Experiment working directory.
        campaign_index: Zero-based campaign index.
        campaign_seed: Derived seed for this campaign.
        status: Campaign status string (e.g., "complete", "failed").
    """
    manifest_path = work_dir / EXPERIMENT_MANIFEST_FILENAME
    data = json.loads(manifest_path.read_text(encoding="utf-8"))

    campaign_entry = {
        "campaign_index": campaign_index,
        "campaign_seed": campaign_seed,
        "status": status,
        "updated_at": datetime.now(UTC).isoformat(),
    }

    # Update existing entry or append new one
    campaigns = data.get("campaigns", [])
    updated = False
    for i, c in enumerate(campaigns):
        if c.get("campaign_index") == campaign_index:
            campaigns[i] = campaign_entry
            updated = True
            break
    if not updated:
        campaigns.append(campaign_entry)

    data["campaigns"] = campaigns
    _atomic_write_json(manifest_path, data)
