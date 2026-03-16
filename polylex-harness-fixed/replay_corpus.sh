#!/usr/bin/env bash
#
# Feeds every input in AFL++'s final corpus through polylex_replay,
# accumulating coverage across all inputs.
#
# Usage:
#   ./replay_corpus.sh [afl-out-dir]
#
# Default corpus directory is ./afl-out/default/queue
# Override by passing a path as the first argument.

set -euo pipefail

CORPUS_DIR="${1:-afl-out/default/queue}"
BINARY="./polylex_replay"

if [ ! -x "$BINARY" ]; then
    echo "ERROR: $BINARY not found or not executable."
    echo "Build it with: make replay"
    exit 1
fi

if [ ! -d "$CORPUS_DIR" ]; then
    echo "ERROR: Corpus directory '$CORPUS_DIR' not found."
    exit 1
fi

# Count inputs
TOTAL=$(find "$CORPUS_DIR" -maxdepth 1 -name 'id:*' | wc -l)
if [ "$TOTAL" -eq 0 ]; then
    echo "ERROR: No corpus inputs found in '$CORPUS_DIR'."
    exit 1
fi

echo "[replay] Found $TOTAL corpus inputs in $CORPUS_DIR"
echo "[replay] Running single replay pass to accumulate coverage..."

# Single run: cat all inputs together separated by newlines.
# polylex reads until EOF so concatenating is fine for coverage purposes —
# we just want to exercise all paths, not get per-input token output.
find "$CORPUS_DIR" -maxdepth 1 -name 'id:*' -print0 \
    | sort -z \
    | xargs -0 cat \
    | "$BINARY" > /dev/null 2>/dev/null || true

echo "[replay] Done."
