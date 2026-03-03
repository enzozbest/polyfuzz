class PipelineError(Exception):
    """Raised when a pipeline stage fails."""

    def __init__(self, stage_name: str, exit_code: int, stderr: str, stdout: str) -> None:
        self.stage_name = stage_name
        self.exit_code = exit_code
        self.stderr = stderr
        self.stdout = stdout
        super().__init__(
            f"Stage '{stage_name}' failed with exit code {exit_code}: {stderr}"
        )


class PreflightError(Exception):
    """Raised when pre-flight checks detect missing or invalid components."""

    def __init__(self, errors: list[str]) -> None:
        self.errors = errors
        super().__init__(self._format_errors())

    def _format_errors(self) -> str:
        header = f"Pre-flight check failed with {len(self.errors)} error(s):"
        items = "\n".join(f"  - {e}" for e in self.errors)
        return f"{header}\n{items}"
