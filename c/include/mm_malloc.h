/* Arbace's <mm_malloc.h> (c/README.md): Intel's aligned allocation, over POSIX's posix_memalign (musl). */
#ifndef __ARBACE_MM_MALLOC_H
#define __ARBACE_MM_MALLOC_H
#include <stdlib.h>
static __inline__ void *_mm_malloc(__SIZE_TYPE__ __size, __SIZE_TYPE__ __align) {
  void *__p;
  if (__align < sizeof(void *)) __align = sizeof(void *);
  return posix_memalign(&__p, __align, __size) == 0 ? __p : (void *)0;
}
static __inline__ void _mm_free(void *__p) { free(__p); }
#endif
