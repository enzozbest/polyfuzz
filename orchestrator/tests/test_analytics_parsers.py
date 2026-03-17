"""Tests for analytics parsers: fuzzer_stats, plot_data, diffcomp reports."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from polyfuzz_orchestrator.analytics.parsers import (
    parse_coverage_summary,
    parse_diffcomp_reports,
    parse_fuzzer_stats,
)

# ---------------------------------------------------------------------------
# parse_fuzzer_stats tests
# ---------------------------------------------------------------------------


class TestParseFuzzerStats:
    """Tests for AFL++ fuzzer_stats key: value parsing."""

    def test_parses_percentage_field_as_float(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text("bitmap_cvg        : 45.23%\n")
        result = parse_fuzzer_stats(stats_file)
        assert result["bitmap_cvg"] == pytest.approx(45.23)

    def test_parses_integer_field(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text("edges_found       : 1234\n")
        result = parse_fuzzer_stats(stats_file)
        assert result["edges_found"] == 1234
        assert isinstance(result["edges_found"], int)

    def test_parses_float_field(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text("execs_per_sec     : 1200.50\n")
        result = parse_fuzzer_stats(stats_file)
        assert result["execs_per_sec"] == pytest.approx(1200.50)

    def test_parses_string_field(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text("afl_banner        : test_target\n")
        result = parse_fuzzer_stats(stats_file)
        assert result["afl_banner"] == "test_target"

    def test_skips_blank_lines(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text("edges_found : 100\n\n\ncorpus_count : 50\n")
        result = parse_fuzzer_stats(stats_file)
        assert result["edges_found"] == 100
        assert result["corpus_count"] == 50

    def test_skips_lines_without_colons(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text("some garbage line\nedges_found : 42\n")
        result = parse_fuzzer_stats(stats_file)
        assert len(result) == 1
        assert result["edges_found"] == 42

    def test_returns_empty_dict_for_missing_file(self, tmp_path: Path) -> None:
        result = parse_fuzzer_stats(tmp_path / "nonexistent")
        assert result == {}

    def test_returns_empty_dict_for_empty_file(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text("")
        result = parse_fuzzer_stats(stats_file)
        assert result == {}

    def test_parses_multiple_fields(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text(
            "start_time        : 1709481600\n"
            "bitmap_cvg        : 45.23%\n"
            "edges_found       : 1234\n"
            "corpus_count      : 567\n"
            "stability         : 99.87%\n"
            "execs_per_sec     : 1200.50\n"
        )
        result = parse_fuzzer_stats(stats_file)
        assert result["start_time"] == 1709481600
        assert result["bitmap_cvg"] == pytest.approx(45.23)
        assert result["edges_found"] == 1234
        assert result["corpus_count"] == 567
        assert result["stability"] == pytest.approx(99.87)
        assert result["execs_per_sec"] == pytest.approx(1200.50)

    def test_strips_whitespace_from_keys_and_values(self, tmp_path: Path) -> None:
        stats_file = tmp_path / "fuzzer_stats"
        stats_file.write_text("  edges_found  :  42  \n")
        result = parse_fuzzer_stats(stats_file)
        assert result["edges_found"] == 42


# ---------------------------------------------------------------------------
# parse_diffcomp_reports tests
# ---------------------------------------------------------------------------


class TestParseDiffcompReports:
    """Tests for diffcomp JSON report parsing."""

    def test_counts_match_diff_failure(self, tmp_path: Path) -> None:
        diffcomp_dir = tmp_path / "diffcomp_output"
        diffcomp_dir.mkdir()

        (diffcomp_dir / "file1.json").write_text(
            json.dumps({"status": "MATCH", "mismatchCount": 0})
        )
        (diffcomp_dir / "file2.json").write_text(
            json.dumps({"status": "DIFF", "mismatchCount": 2})
        )
        (diffcomp_dir / "file3.json").write_text(
            json.dumps({"status": "FAILURE", "error": "lexer error"})
        )

        match, diff, failure = parse_diffcomp_reports(diffcomp_dir)
        assert match == 1
        assert diff == 1
        assert failure == 1

    def test_returns_zeros_for_missing_directory(self, tmp_path: Path) -> None:
        result = parse_diffcomp_reports(tmp_path / "nonexistent")
        assert result == (0, 0, 0)

    def test_returns_zeros_for_empty_directory(self, tmp_path: Path) -> None:
        diffcomp_dir = tmp_path / "diffcomp_output"
        diffcomp_dir.mkdir()
        result = parse_diffcomp_reports(diffcomp_dir)
        assert result == (0, 0, 0)

    def test_skips_malformed_json_as_failure(self, tmp_path: Path) -> None:
        diffcomp_dir = tmp_path / "diffcomp_output"
        diffcomp_dir.mkdir()

        (diffcomp_dir / "good.json").write_text(
            json.dumps({"status": "MATCH", "mismatchCount": 0})
        )
        (diffcomp_dir / "bad.json").write_text("not valid json {{{")

        match, diff, failure = parse_diffcomp_reports(diffcomp_dir)
        assert match == 1
        assert diff == 0
        assert failure == 1

    def test_unknown_status_counts_as_failure(self, tmp_path: Path) -> None:
        diffcomp_dir = tmp_path / "diffcomp_output"
        diffcomp_dir.mkdir()

        (diffcomp_dir / "file1.json").write_text(
            json.dumps({"status": "UNKNOWN_STATUS"})
        )

        match, diff, failure = parse_diffcomp_reports(diffcomp_dir)
        assert match == 0
        assert diff == 0
        assert failure == 1

    def test_only_reads_json_files(self, tmp_path: Path) -> None:
        diffcomp_dir = tmp_path / "diffcomp_output"
        diffcomp_dir.mkdir()

        (diffcomp_dir / "file1.json").write_text(
            json.dumps({"status": "MATCH"})
        )
        (diffcomp_dir / "notes.txt").write_text("this should be ignored")

        match, diff, failure = parse_diffcomp_reports(diffcomp_dir)
        assert match == 1
        assert diff == 0
        assert failure == 0

    def test_multiple_match_and_diff(self, tmp_path: Path) -> None:
        diffcomp_dir = tmp_path / "diffcomp_output"
        diffcomp_dir.mkdir()

        for i in range(5):
            (diffcomp_dir / f"match_{i}.json").write_text(
                json.dumps({"status": "MATCH"})
            )
        for i in range(3):
            (diffcomp_dir / f"diff_{i}.json").write_text(
                json.dumps({"status": "DIFF", "mismatchCount": i + 1})
            )

        match, diff, failure = parse_diffcomp_reports(diffcomp_dir)
        assert match == 5
        assert diff == 3
        assert failure == 0


# ---------------------------------------------------------------------------
# parse_coverage_summary tests
# ---------------------------------------------------------------------------


class TestParseCoverageSummary:
    """Tests for coverage_summary.json parsing."""

    def test_parses_valid_summary(self, tmp_path: Path) -> None:
        summary_file = tmp_path / "coverage_summary.json"
        data = {
            "total_branches": 101,
            "covered_branches": 80,
            "branch_coverage_pct": 79.21,
            "uncovered_ids": [5, 10, 15],
        }
        summary_file.write_text(json.dumps(data))
        result = parse_coverage_summary(summary_file)
        assert result["total_branches"] == 101
        assert result["covered_branches"] == 80
        assert result["branch_coverage_pct"] == pytest.approx(79.21)
        assert result["uncovered_ids"] == [5, 10, 15]

    def test_returns_empty_dict_for_missing_file(self, tmp_path: Path) -> None:
        result = parse_coverage_summary(tmp_path / "nonexistent.json")
        assert result == {}

    def test_returns_empty_dict_for_malformed_json(self, tmp_path: Path) -> None:
        bad_file = tmp_path / "coverage_summary.json"
        bad_file.write_text("not json {{{")
        result = parse_coverage_summary(bad_file)
        assert result == {}
