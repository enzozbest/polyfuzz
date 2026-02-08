/*
 * AFL++ Coverage Shim for PolyLex
 * Compile with:
 *   afl-clang-fast -c afl_shim.c -o afl_shim.o
 *
 * Then link with the polylex object file (compiled with polyc):
 *   afl-clang-fast -o fuzz_polylex polylex_fuzz.o afl_shim.o \
 *       -L<polyml_lib_path> -Wl,-rpath,<polyml_lib_path> \
 *       -lpolymain -lpolyml
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/shm.h>
#include <sys/types.h>

//AFL shared memory environment variable
#define SHM_ENV_VAR "__AFL_SHM_ID"

//Standard AFL bitmap size (64KB
#define MAP_SIZE_POW2 16
#define MAP_SIZE (1 << MAP_SIZE_POW2)

//Pointer to AFL shared memory bitmap
static uint8_t *afl_area_ptr = NULL;

//Previous edge location
static uint16_t prev_loc = 0;

/*
 * Map the AFL++ shared memory region.
 * Must be called once at startup before any trace calls.
 * (Called from SML via FFI).
 */
void setup(void) {
    char *shm_id_str = getenv(SHM_ENV_VAR);
    if (shm_id_str) {
        int shm_id = atoi(shm_id_str);
        afl_area_ptr = (uint8_t *) shmat(shm_id, NULL, 0);
        if (afl_area_ptr == (void *) -1) {
            fprintf(stderr, "Failed to attach shared memory!\n");
            afl_area_ptr = NULL;
        }
    }

    //Fallback: dummy map
    if (!afl_area_ptr) {
        static uint8_t dummy_map[MAP_SIZE];
        memset(dummy_map, 0, MAP_SIZE);
        afl_area_ptr = dummy_map;
    }
}

/*
 * Log an edge transition.
 * Called from SML at each branch point via FFI.
 *
 * SML usage:
 *   val trace = Foreign.buildCall1 (sym, Foreign.cInt, Foreign.cVoid)
 *   trace id;  (* unique ID per branch *)
 */
void trace(int edge_id) {
    uint16_t cur = (uint16_t) (edge_id & 0xFFFF);
    if (afl_area_ptr) {
        afl_area_ptr[cur ^ prev_loc]++;
        prev_loc = cur >> 1;
    }
}

/*
 * Reset edge tracking state. Call between fuzz iterations.
 */
void reset(void) {
    prev_loc = 0;
}
