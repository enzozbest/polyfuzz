# PolyFuzz 

[![DOI](https://zenodo.org/badge/1121033071.svg)](https://doi.org/10.5281/zenodo.19593518)

Differential testing infrastructure for the [PolyML](https://polyml.org) lexer. PolyFuzz automatically generates SML source files, lexes them with both PolyML and an independent reference lexer ([Verilex](verilex/)), and reports any discrepancies in token output.

## Components

| Component | Language | Purpose |
|---|---|---|
| **[smlgen](smlgen/)** | Kotlin | Generates randomised SML test corpus |
| **[polylex-harness](polylex-harness/)** | SML / C | AFL++ instrumented harness around the PolyML lexer |
| **[verilex](verilex/)** | Kotlin | Independent reference lexer for SML |
| **[diffcomp](diffcomp/)** | Kotlin | Token-level differential comparison and reporting |
| **[orchestrator](orchestrator/)** | Python | End-to-end pipeline orchestration and analytics |

## Prerequisites

- JDK 21+ (for smlgen, verilex, and diffcomp via Gradle)
- Python 3.13+ and [uv](https://docs.astral.sh/uv/)
- PolyML (for the lexer harness)
- AFL++ (optional, for fuzz testing)

## Quick start

```bash
# Initialise submodules
git submodule update --init --recursive

# Build everything
make

# Verify build artifacts
make check
```

## Usage

The `./polyfuzz` wrapper script at the repo root is the main entry point. It handles dependency installation automatically.

```bash
# Run a single campaign (100 tests, random seed)
./polyfuzz -d ./results -n 100

# Run 5 campaigns with a fixed seed
./polyfuzz -d ./results -n 200 -s 42 -N 5

# Run without AFL (direct corpus comparison)
./polyfuzz -d ./results -n 100 --no-afl

# Run a single pipeline stage
./polyfuzz -d ./results run-stage smlgen

# Analyse existing results
./polyfuzz -d ./results analyse
```

Run `./polyfuzz --help` for the full set of options.

## License

[MIT](LICENSE)
