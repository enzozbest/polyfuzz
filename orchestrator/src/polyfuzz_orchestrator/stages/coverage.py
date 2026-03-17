"""Coverage replay stage: run polylex_replay on AFL queue and produce branch coverage summary."""

from __future__ import annotations

import json
import os
import re
from pathlib import Path

from polyfuzz_orchestrator.config import PipelineConfig
from polyfuzz_orchestrator.errors import PreflightError
from polyfuzz_orchestrator.process import ProcessRunner, StageResult
from polyfuzz_orchestrator.stages.validation import validate_path, validate_single
from polyfuzz_orchestrator.stages.base import Stage
from polyfuzz_orchestrator.stages.diffcomp import DiffcompStage


class CoverageStage(Stage):
    """Replay AFL queue inputs through polylex_replay to collect branch-level coverage.

    Reads all AFL queue files, concatenates them, pipes via stdin to polylex_replay,
    then parses the coverage log to produce a coverage summary JSON.
    """

    @property
    def name(self) -> str:
        return "coverage"

    def validate(self, campaign_dir: Path, config: PipelineConfig) -> None:
        """Verify polylex_replay exists and is executable, LEX_.ML exists, and AFL queue exists."""
        queue_dir = DiffcompStage._find_queue_dir(campaign_dir)
        polylex_replay_errors = validate_single(config.polylex_replay_bin, "polylex_replay")
        lex_ml_errors = [
            x for x in [validate_path(config.lex_ml_path, "LEX_.ML")] if x is not None
        ]  # Trick using comprehensions for null check!
        queue_errors = [] if queue_dir else ["AFL++ queue directory not found"]

        errors = [*polylex_replay_errors, *lex_ml_errors, *queue_errors]
        if errors:
            raise PreflightError(errors)

    def execute(
        self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
    ) -> StageResult:
        """Run polylex_replay on concatenated AFL queue inputs and produce coverage summary.

        1. Find AFL queue dir and read all queue files.
        2. Concatenate input data and pipe to polylex_replay via stdin.
        3. Parse coverage_out/coverage.log for fired trace IDs.
        4. Parse LEX_.ML for all known trace IDs.
        5. Write coverage_out/coverage_summary.json.
        """
        coverage_out = campaign_dir / "coverage_out"
        coverage_out.mkdir(parents=True, exist_ok=True)

        # 1. Read and concatenate queue files
        queue_dir = DiffcompStage._find_queue_dir(campaign_dir)
        input_data = self._concatenate_queue_files(queue_dir)

        # 2. Run polylex_replay
        cmd = [str(config.polylex_replay_bin.resolve())]
        result = runner.run(
            cmd=cmd,
            stage_name=self.name,
            output_dir=coverage_out,
            timeout_s=config.stage_timeout_s,
            cwd=campaign_dir,
            input_data=input_data,
        )

        # 3. Parse coverage log
        coverage_log = coverage_out / "coverage.log"
        fired_ids = self._parse_coverage_log(coverage_log)

        # 4. Parse LEX_.ML for known IDs
        all_ids = self._parse_known_ids(config.lex_ml_path)

        # 5. Write summary
        total_branches = len(all_ids)
        covered_branches = len(fired_ids & all_ids) if all_ids else len(fired_ids)
        branch_coverage_pct = (
            (covered_branches / total_branches * 100.0) if total_branches > 0 else 0.0
        )
        uncovered_ids = sorted(all_ids - fired_ids)

        summary = {
            "total_branches": total_branches,
            "covered_branches": covered_branches,
            "branch_coverage_pct": round(branch_coverage_pct, 2),
            "uncovered_ids": uncovered_ids,
        }

        (coverage_out / "coverage_summary.json").write_text(
            json.dumps(summary, indent=2), encoding="utf-8"
        )

        return result

    @staticmethod
    def _concatenate_queue_files(queue_dir: Path | None) -> bytes:
        """Read and concatenate all queue files, skipping dotfiles and README.txt."""
        if queue_dir is None:
            return b""
        chunks: list[bytes] = []
        for f in sorted(queue_dir.iterdir()):
            if f.is_file() and not f.name.startswith(".") and f.name != "README.txt":
                chunks.append(f.read_bytes())
        return b"\n".join(chunks)

    @staticmethod
    def _parse_coverage_log(coverage_log: Path) -> set[int]:
        """Parse coverage.log: one fired trace ID per line."""
        fired: set[int] = set()
        try:
            text = coverage_log.read_text(encoding="utf-8")
        except (FileNotFoundError, OSError):
            return fired
        for line in text.splitlines():
            line = line.strip()
            if line:
                try:
                    fired.add(int(line))
                except ValueError:
                    continue
        return fired

    @staticmethod
    def _parse_known_ids(lex_ml_path: Path) -> set[int]:
        """Parse LEX_.ML for all aflTrace IDs using regex."""
        try:
            text = lex_ml_path.read_text(encoding="utf-8")
        except (FileNotFoundError, OSError):
            return set()
        return {int(m.group(1)) for m in re.finditer(r"aflTrace\s+(\d+)", text)}
