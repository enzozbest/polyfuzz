"""Tests for campaign directory layout creation."""

from pathlib import Path

from polyfuzz_orchestrator.layout import create_campaign_layout


def test_create_campaign_layout_creates_directories(tmp_path: Path) -> None:
    """create_campaign_layout creates corpus, afl_output, diffcomp_input, diffcomp_output, results subdirectories."""
    result = create_campaign_layout(tmp_path)

    expected_dirs = ["corpus", "afl_output", "diffcomp_input", "diffcomp_output", "coverage_out", "results"]
    for dir_name in expected_dirs:
        assert (tmp_path / dir_name).is_dir(), f"Expected directory {dir_name} to exist"


def test_create_campaign_layout_returns_path_dict(tmp_path: Path) -> None:
    """create_campaign_layout returns dict mapping directory names to Path objects."""
    result = create_campaign_layout(tmp_path)

    assert isinstance(result, dict)
    expected_keys = {"corpus", "afl_output", "diffcomp_input", "diffcomp_output", "coverage_out", "results"}
    assert set(result.keys()) == expected_keys

    for name, path in result.items():
        assert isinstance(path, Path)
        assert path == tmp_path / name
        assert path.is_dir()
