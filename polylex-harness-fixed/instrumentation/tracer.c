#include <stdint.h>
#include <stdlib.h>
#include <sys/shm.h>


uint8_t *__afl_area_ptr;
uint32_t __afl_map_size = 65536;

__attribute__((constructor))
void __afl_init(void) {
    char *shm_id_str = getenv("__AFL_SHM_ID");
    if (shm_id_str) {
        int shm_id = atoi(shm_id_str);
        __afl_area_ptr = shmat(shm_id, NULL, 0);
    }
}

void trace(int id) {
    if (__afl_area_ptr) {
        __afl_area_ptr[id % __afl_map_size]++;
    }
}
