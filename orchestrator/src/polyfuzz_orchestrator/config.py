from __future__ import annotations

import tomllib
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

# Fields that should be converted from strings to Path objects
_PATH_FIELDS = frozenset({"work_dir", "smlgen_jar", "polylex_bin", "diffcomp_bin", "afl_fuzz_bin"})


@dataclass(frozen=True)
class PipelineConfig:
    """Validated pipeline configuration.

    All timeout values are in seconds unless otherwise noted.
    """

    work_dir: Path
    tests_per_campaign: int = 100
    seed: int | None = None  # None = random
    num_campaigns: int = 1  # Number of campaigns to run (1 = single-campaign mode)
    afl_timeout_s: int = 10  # AFL++ campaign duration via -V flag
    afl_exec_timeout_ms: int | None = None  # Per-input timeout via -t, None = auto-calibrate
    stage_timeout_s: int = 600  # Orchestrator subprocess timeout

    # Paths to components (discovered or configured)
    smlgen_jar: Path = field(default_factory=lambda: Path("smlgen/build/install/smlgen/bin/smlgen"))
    polylex_bin: Path = field(default_factory=lambda: Path("polylex-harness/polylex_fuzz"))
    diffcomp_bin: Path = field(default_factory=lambda: Path("diffcomp/build/install/diffcomp/bin/diffcomp"))
    afl_fuzz_bin: Path = field(default_factory=lambda: Path("/usr/local/bin/afl-fuzz"))

    def __post_init__(self) -> None:
        """Validate timeout hierarchy."""
        if self.stage_timeout_s <= self.afl_timeout_s:
            msg = (
                f"stage_timeout_s ({self.stage_timeout_s}) must be greater than "
                f"afl_timeout_s ({self.afl_timeout_s}) to avoid killing AFL++ prematurely"
            )
            raise ValueError(msg)


def load_config(toml_path: Path | None, **cli_overrides: Any) -> PipelineConfig:
    """Load config from TOML file, then apply CLI overrides.

    Reads the [polyfuzz] section from the TOML file if provided. CLI overrides with non-None values take precedence
    over TOML values. String values for path fields are converted to Path objects.
    """
    base: dict[str, Any] = {}

    if toml_path is not None:
        with open(toml_path, "rb") as f:
            raw = tomllib.load(f)
        base = dict(raw.get("polyfuzz", {}))

    # CLI overrides replace TOML values (filter out None = "not provided")
    merged = {**base, **{k: v for k, v in cli_overrides.items() if v is not None}}

    # Convert string values to Path objects for path fields
    for key in _PATH_FIELDS:
        if key in merged and isinstance(merged[key], str):
            merged[key] = Path(merged[key])

    return PipelineConfig(**merged)
