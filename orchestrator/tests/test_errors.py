"""Tests for error types."""

from polyfuzz_orchestrator.errors import PipelineError, PreflightError


def test_pipeline_error_stores_attributes() -> None:
    """PipelineError stores stage_name, exit_code, stderr attributes."""
    err = PipelineError(stage_name="smlgen", exit_code=1, stderr="something failed", stdout="partial output")

    assert err.stage_name == "smlgen"
    assert err.exit_code == 1
    assert err.stderr == "something failed"
    assert err.stdout == "partial output"


def test_pipeline_error_is_exception() -> None:
    """PipelineError is an Exception."""
    err = PipelineError(stage_name="afl", exit_code=2, stderr="timeout", stdout="")
    assert isinstance(err, Exception)


def test_preflight_error_stores_error_list() -> None:
    """PreflightError stores a list of error strings."""
    errors = ["smlgen.jar not found", "afl-fuzz not executable"]
    err = PreflightError(errors=errors)

    assert err.errors == errors
    assert isinstance(err, Exception)


def test_preflight_error_str_lists_all_errors() -> None:
    """PreflightError __str__ lists all errors in a human-readable format."""
    errors = ["smlgen.jar not found", "afl-fuzz not executable"]
    err = PreflightError(errors=errors)

    error_str = str(err)
    for error_msg in errors:
        assert error_msg in error_str


def test_pipeline_error_message_includes_diagnostics() -> None:
    """PIPE-04: PipelineError string includes stage name, exit code, and stderr for diagnostics."""
    err = PipelineError(
        stage_name="afl",
        exit_code=2,
        stderr="segfault at 0x0",
        stdout="partial output",
    )
    msg = str(err)
    assert "afl" in msg, "PipelineError message must include stage name"
    assert "2" in msg, "PipelineError message must include exit code"
    assert "segfault" in msg, "PipelineError message must include stderr content"


def test_preflight_error_includes_all_errors_in_message() -> None:
    """PIPE-03 support: PreflightError message includes all component errors for diagnostics."""
    errors = [
        "smlgen JAR not found at smlgen/build/libs/smlgen.jar",
        "polylex binary not found at polylex-harness/polylex_fuzz",
    ]
    err = PreflightError(errors=errors)
    msg = str(err)
    for error_text in errors:
        assert error_text in msg, f"PreflightError message must include: {error_text}"
    assert "2 error(s)" in msg, "PreflightError message must include error count"
