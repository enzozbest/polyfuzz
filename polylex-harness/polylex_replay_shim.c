/*
 * Replay Shim for PolyLex - Coverage Insights
 *
 * A drop-in replacement for polylex_c_shim.c for use outside AFL++.
 * Implements the same setup/trace/reset interface but records which
 * trace IDs fired across the entire run, then writes a coverage report
 * to coverage_out/coverage.log on exit.
 *
 * Build:
 *   cc -c polylex_replay_shim.c -o polylex_replay_shim.o
 *
 * Link:
 *   cc -o polylex_replay polylex_fuzz.o polylex_replay_shim.o \
 *       -L/usr/local/lib -Wl,-rpath,/usr/local/lib \
 *       -lpolymain -lpolyml
 *
 * Usage:
 *   ./replay_corpus.sh          # feeds corpus through polylex_replay
 *   python3 coverage_report.py  # reports coverage against known IDs
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <sys/stat.h>

#define MAX_ID 65536

/* Bitset: seen[id] = 1 if aflTrace(id) was called at least once */
static uint8_t seen[MAX_ID];

#define OUT_DIR  "coverage_out"
#define OUT_FILE "coverage_out/coverage.log"

/*
 * Write all fired IDs to coverage_out/coverage.log, one per line.
 * Registered with atexit() so it runs even if the SML side exits early.
 */
static void dump_coverage(void) {
    mkdir(OUT_DIR, 0755); //Ensure directory exists

    FILE *f = fopen(OUT_FILE, "w");
    if (!f) {
        fprintf(stderr, "[replay] WARNING: could not open %s for writing\n", OUT_FILE);
        return;
    }

    int count = 0;
    for (int i = 0; i < MAX_ID; i++) {
        if (seen[i]) {
            fprintf(f, "%d\n", i);
            count++;
        }
    }
    fclose(f);
    fprintf(stderr, "[replay] Coverage: %d unique trace IDs fired. Written to %s\n",
            count, OUT_FILE);
}

/*
 * setup() — called once from SML at startup via FFI.
 * Clears the seen array and registers the exit handler.
 */
void setup(void) {
    memset(seen, 0, sizeof(seen));
    atexit(dump_coverage);
}

/*
 * trace(edge_id) — called from SML at each instrumented branch via FFI.
 * Records that this ID was reached.
 */
void trace(int edge_id) {
    if (edge_id >= 0 && edge_id < MAX_ID)
        seen[edge_id] = 1;
}

/*
 * reset() — called from SML between inputs via FFI.
 * In the replay shim we intentionally do NOT reset seen[] between inputs —
 * we want to accumulate coverage across the entire corpus replay.
 */
void reset(void) {
    /* deliberately empty */
}
