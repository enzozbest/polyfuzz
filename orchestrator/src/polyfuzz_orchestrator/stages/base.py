from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from polyfuzz_orchestrator.config import PipelineConfig
    from polyfuzz_orchestrator.process import ProcessRunner, StageResult


class Stage(ABC):
    """Abstract base class for pipeline stages.

    Each stage validates preconditions and executes an external tool via a subprocess. Stages communicate
    through the filesystem (campaign directory).
    """

    @property
    @abstractmethod
    def name(self) -> str:
        """Unique stage identifier (e.g. 'smlgen', 'afl', 'diffcomp')."""
        ...

    @abstractmethod
    def validate(self, campaign_dir: Path, config: PipelineConfig) -> None:
        """Check preconditions before execution.

        Raises PreflightError if preconditions are not met (e.g. missing binaries, empty directories).
        """
        ...

    @abstractmethod
    def execute(
        self, campaign_dir: Path, config: PipelineConfig, runner: ProcessRunner
    ) -> StageResult:
        """Run the stage and return a StageResult.

        The runner parameter provides subprocess invocation with timing and timeout handling.
        """
        ...
