"""Validation helpers for pipeline stages."""

from os import X_OK, access
from pathlib import Path


def validate_path(path: Path, name: str | None) -> str | None:
    if not path.exists():
        return f"{name if name else ''} not found at {path.name}!"
    return None


def validate_executable_perm(path: Path, name: str | None) -> str | None:
    if not access(path, X_OK):
        return f"{name if name else ''} not executable at {path.name}"
    return None


def validate_sml_files_exist(path: Path, name: str | None) -> str | None:
    sml_files = list(path.glob("*.sml"))
    all_files = list(path.iterdir())
    if not sml_files and not all_files:
        return f"{name if name else ''} directory at {path.name} is empty"
    return None


def validate_single(path: Path, name: str | None = None) -> list[str]:
    errors = [
        validate_path(path, name),
        validate_executable_perm(path, name),
    ]

    return [x for x in errors if x is not None]
