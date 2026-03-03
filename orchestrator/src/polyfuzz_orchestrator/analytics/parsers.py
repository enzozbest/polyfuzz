"""Parsers for AFL++ fuzzer_stats, plot_data, and diffcomp JSON reports.

Pure functions that operate on file paths and return typed data.
All handle missing or malformed input gracefully (return empty results, never raise).
"""

from __future__ import annotations

import json
import logging
from pathlib import Path

logger = logging.getLogger(__name__)

# AFL++ plot_data standard columns (15 columns).
# Source: AFL++ src/afl-fuzz-init.c setup_dirs_fds()
PLOT_DATA_COLUMNS = [
    "relative_time",
    "cycles_done",
    "cur_item",
    "corpus_count",
    "pending_total",
    "pending_favs",
    "map_size",
    "saved_crashes",
    "saved_hangs",
    "max_depth",
    "execs_per_sec",
    "total_execs",
    "edges_found",
    "total_crashes",
    "servers_count",
]


def parse_fuzzer_stats(stats_path: Path) -> dict[str, str | int | float]:
    """Parse AFL++ fuzzer_stats key: value file into a typed dictionary.

    Args:
        stats_path: Path to the fuzzer_stats file.
    Returns:
        Dictionary mapping stat names to their parsed values.
    """
    result: dict[str, str | int | float] = {}
    try:
        text = stats_path.read_text(encoding="utf-8")
    except (FileNotFoundError, OSError) as exc:
        logger.debug("Cannot read fuzzer_stats at %s: %s", stats_path, exc)
        return result

    for line in text.splitlines():
        if ":" not in line:
            continue
        key, _, value = line.partition(":")
        key = key.strip()
        value = value.strip()
        if not key:
            continue

        if value.endswith("%"):
            try:
                result[key] = float(value[:-1])
            except ValueError:
                result[key] = value
        else:
            try:
                result[key] = int(value)
            except ValueError:
                try:
                    result[key] = float(value)
                except ValueError:
                    result[key] = value

    return result


def parse_plot_data(plot_data_path: Path) -> list[dict[str, float]]:
    """Parse AFL++ plot_data CSV into a list of dicts keyed by column name.

    Args:
        plot_data_path: Path to the plot_data file.

    Returns:
        List of row dictionaries with float values for each known column.
    """
    rows: list[dict[str, float]] = []
    try:
        text = plot_data_path.read_text(encoding="utf-8")
    except (FileNotFoundError, OSError) as exc:
        logger.debug("Cannot read plot_data at %s: %s", plot_data_path, exc)
        return rows

    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue

        parts = [p.strip() for p in line.split(",")]
        row: dict[str, float] = {}
        for i, col_name in enumerate(PLOT_DATA_COLUMNS):
            if i < len(parts):
                try:
                    row[col_name] = float(parts[i])
                except ValueError:
                    row[col_name] = 0.0
            else:
                row[col_name] = 0.0
        rows.append(row)

    return rows


def parse_diffcomp_reports(diffcomp_dir: Path) -> tuple[int, int, int]:
    """Parse all diffcomp JSON reports in a directory.

    Counts files by status: MATCH, DIFF, FAILURE. Malformed JSON or unknown statuses count as FAILURE.
    Args:
        diffcomp_dir: Path to directory containing per-file JSON reports.
    Returns:
        Tuple of (match_count, diff_count, failure_count).
    """
    match_count = 0
    diff_count = 0
    failure_count = 0

    try:
        json_files = sorted(diffcomp_dir.glob("*.json"))
    except (FileNotFoundError, OSError) as exc:
        logger.debug("Cannot read diffcomp directory %s: %s", diffcomp_dir, exc)
        return 0, 0, 0

    for json_file in json_files:
        try:
            data = json.loads(json_file.read_text(encoding="utf-8"))
            status = data.get("status", "FAILURE")
            if status == "MATCH":
                match_count += 1
            elif status == "DIFF":
                diff_count += 1
            else:
                failure_count += 1
        except (json.JSONDecodeError, OSError) as exc:
            logger.debug("Malformed diffcomp report %s: %s", json_file, exc)
            failure_count += 1

    return match_count, diff_count, failure_count
