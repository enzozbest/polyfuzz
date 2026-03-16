#!/usr/bin/env python3
"""
coverage_report.py

Reads coverage_out/coverage.log (produced by polylex_replay via replay_corpus.sh)
and reports coverage against the known set of instrumented trace IDs in LEX_.ML.

Usage:
    python3 coverage_report.py [--lex LEX_.ML] [--log coverage_out/coverage.log]
"""

import argparse
import re
import sys
from pathlib import Path

# ── CLI ───────────────────────────────────────────────────────────────────────
parser = argparse.ArgumentParser(description="PolyLex branch coverage report")
parser.add_argument("--lex", default="LEX_.ML", help="Instrumented SML lexer file")
parser.add_argument(
    "--log", default="coverage_out/coverage.log", help="Coverage log from replay run"
)
args = parser.parse_args()

# ── Load known IDs from LEX_.ML ───────────────────────────────────────────────
lex_path = Path(args.lex)
if not lex_path.exists():
    print(f"ERROR: {lex_path} not found.", file=sys.stderr)
    sys.exit(1)

lex_src = lex_path.read_text()

# Extract (line_number, id) pairs so we can report location for misses
known = {}  # id -> line number
for i, line in enumerate(lex_src.splitlines(), 1):
    for m in re.finditer(r"aflTrace\s+(\d+)", line):
        known[int(m.group(1))] = i

print(f"Instrumented branch points in {lex_path}: {len(known)}")

# ── Load fired IDs from coverage log ─────────────────────────────────────────
log_path = Path(args.log)
if not log_path.exists():
    print(
        f"ERROR: {log_path} not found. Did you run replay_corpus.sh?", file=sys.stderr
    )
    sys.exit(1)

fired = set()
for line in log_path.read_text().splitlines():
    line = line.strip()
    if line.isdigit():
        fired.add(int(line))

# ── Report ────────────────────────────────────────────────────────────────────
covered = {i for i in known if i in fired}
not_covered = {i for i in known if i not in fired}
unknown = fired - set(known)  # IDs in log but not in LEX_.ML (shouldn't happen)

pct = 100.0 * len(covered) / len(known) if known else 0.0

print(f"Fired trace IDs in log:          {len(fired)}")
print(f"Covered instrumented branches:   {len(covered)} / {len(known)}  ({pct:.1f}%)")
print(f"Uncovered instrumented branches: {len(not_covered)}")

if unknown:
    print(f"WARNING: {len(unknown)} IDs in log not found in LEX_.ML: {sorted(unknown)}")

# ── Uncovered branches ────────────────────────────────────────────────────────
if not_covered:
    print("\nUncovered branches (ID -> line in LEX_.ML):")
    # Group by approximate function by finding nearest fun/val declaration above
    fun_map = {}
    lines = lex_src.splitlines()
    for id_ in sorted(not_covered, key=lambda x: known[x]):
        lineno = known[id_]
        # Walk backwards to find enclosing function name
        func = "<unknown>"
        for j in range(lineno - 2, max(0, lineno - 60), -1):
            m = re.search(r"\bfun\s+(\w+)", lines[j])
            if m:
                func = m.group(1)
                break
        fun_map.setdefault(func, []).append((id_, lineno))

    for func, entries in fun_map.items():
        print(f"\n  {func}:")
        for id_, lineno in entries:
            # Show the source line for context
            src_line = lines[lineno - 1].strip()[:72]
            print(f"    ID {id_:5d}  line {lineno:3d}:  {src_line}")

print(f"\n{'=' * 60}")
print(f"Branch coverage: {len(covered)}/{len(known)} ({pct:.1f}%)")
