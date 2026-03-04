"""Tests for PipelineConfig and load_config."""

from pathlib import Path

import pytest

from polyfuzz_orchestrator.config import PipelineConfig, load_config


class TestPipelineConfigDefaults:
    """Test PipelineConfig construction with defaults."""

    def test_defaults_produce_valid_config(self, tmp_path: Path) -> None:
        """PipelineConfig with all defaults produces valid config with expected values."""
        config = PipelineConfig(work_dir=tmp_path)

        assert config.tests_per_campaign == 100
        assert config.afl_timeout_s == 300
        assert config.stage_timeout_s == 600
        assert config.seed is None
        assert config.afl_exec_timeout_ms is None

    def test_config_is_frozen(self, tmp_path: Path) -> None:
        """PipelineConfig instances are frozen (immutable)."""
        config = PipelineConfig(work_dir=tmp_path)

        with pytest.raises(AttributeError):
            config.tests_per_campaign = 200  # type: ignore[misc]


class TestPipelineConfigValidation:
    """Test PipelineConfig validation."""

    def test_raises_if_stage_timeout_not_greater_than_afl_timeout(
        self, tmp_path: Path
    ) -> None:
        """PipelineConfig raises ValueError if stage_timeout_s <= afl_timeout_s."""
        with pytest.raises(ValueError, match="stage_timeout_s"):
            PipelineConfig(work_dir=tmp_path, afl_timeout_s=300, stage_timeout_s=300)

    def test_raises_if_stage_timeout_less_than_afl_timeout(self, tmp_path: Path) -> None:
        """PipelineConfig raises ValueError if stage_timeout_s < afl_timeout_s."""
        with pytest.raises(ValueError, match="stage_timeout_s"):
            PipelineConfig(work_dir=tmp_path, afl_timeout_s=500, stage_timeout_s=200)


class TestLoadConfig:
    """Test load_config with and without TOML files."""

    def test_load_config_from_toml(self, tmp_path: Path) -> None:
        """load_config from TOML file reads tests_per_campaign and seed correctly."""
        toml_file = tmp_path / "polyfuzz.toml"
        toml_file.write_text(
            '[polyfuzz]\ntests_per_campaign = 200\nseed = 42\n'
        )

        config = load_config(toml_path=toml_file, work_dir=tmp_path)

        assert config.tests_per_campaign == 200
        assert config.seed == 42

    def test_load_config_cli_overrides_toml(self, tmp_path: Path) -> None:
        """load_config merges CLI overrides over TOML values (CLI wins)."""
        toml_file = tmp_path / "polyfuzz.toml"
        toml_file.write_text(
            '[polyfuzz]\ntests_per_campaign = 200\nseed = 42\n'
        )

        config = load_config(
            toml_path=toml_file,
            work_dir=tmp_path,
            tests_per_campaign=500,
        )

        assert config.tests_per_campaign == 500
        # Seed from TOML should still be preserved
        assert config.seed == 42

    def test_load_config_no_toml_uses_defaults_with_overrides(
        self, tmp_path: Path
    ) -> None:
        """load_config with no TOML file uses defaults + CLI overrides."""
        config = load_config(toml_path=None, work_dir=tmp_path, tests_per_campaign=300)

        assert config.tests_per_campaign == 300
        assert config.afl_timeout_s == 300  # default
        assert config.seed is None  # default

    def test_load_config_converts_path_strings(self, tmp_path: Path) -> None:
        """load_config converts path strings from TOML to Path objects."""
        toml_file = tmp_path / "polyfuzz.toml"
        toml_file.write_text(
            '[polyfuzz]\nsmlgen_bin = "custom/path/smlgen.jar"\n'
        )

        config = load_config(toml_path=toml_file, work_dir=tmp_path)

        assert isinstance(config.smlgen_bin, Path)
        assert config.smlgen_bin == Path("custom/path/smlgen.jar")


class TestTomlCliEquivalence:
    """BILD-03: TOML config produces identical PipelineConfig to equivalent CLI flags."""

    def test_toml_and_cli_produce_identical_config(self, tmp_path: Path) -> None:
        """BILD-03: TOML config and equivalent CLI overrides produce matching PipelineConfig fields."""
        toml_file = tmp_path / "polyfuzz.toml"
        toml_file.write_text(
            "[polyfuzz]\n"
            "tests_per_campaign = 200\n"
            "seed = 42\n"
            "afl_timeout_s = 500\n"
        )

        from_toml = load_config(toml_path=toml_file, work_dir=tmp_path)
        from_cli = load_config(
            toml_path=None,
            work_dir=tmp_path,
            tests_per_campaign=200,
            seed=42,
            afl_timeout_s=500,
        )

        assert from_toml.tests_per_campaign == from_cli.tests_per_campaign
        assert from_toml.seed == from_cli.seed
        assert from_toml.afl_timeout_s == from_cli.afl_timeout_s
        assert from_toml.work_dir == from_cli.work_dir
