# PolyFuzz Orchestrator

End-to-end orchestration for PolyFuzz differential testing.

## Installation

This project uses `uv` for dependency management. To install and run:

```bash
# Clone the repository
git clone <repository-url>
cd orchestrator

# Install dependencies and the package
uv sync
```

## Usage

The primary entry point is the `polyfuzz` command.

### Basic Execution

To run a single fuzzing campaign in a working directory:

```bash
uv run polyfuzz --work-dir ./results
```

### Multiple Campaigns

To run multiple campaigns (e.g., 5) with a specific number of tests (e.g., 500) per campaign:

```bash
uv run polyfuzz --work-dir ./results --campaigns 5 --tests 500
```

### Commands and Options

- `--work-dir`, `-d`: (Required) Directory where campaign results and manifests will be stored.
- `--campaigns`, `-N`: Number of campaigns to run (default: 1).
- `--tests`, `-n`: Number of tests to generate per campaign (default: 100).
- `--seed`, `-s`: Master seed for reproducibility.
- `--no-analytics`: Skip automatic analytics after campaigns finish.
- `--help`: Show all available options and commands.

### Subcommands

- `analyse`: Run analytics on an existing results directory.
  ```bash
  uv run polyfuzz --work-dir ./results analyse
  ```
- `only-smlgen`: Run only the SML generator stage.
- `only-afl`: Run only the AFL++ fuzzing stage.
- `only-diffcomp`: Run only the differential comparison stage.

## Development

Run tests with pytest:

```bash
uv run pytest
```

Check code quality with ruff:

```bash
uv run ruff check .
```
