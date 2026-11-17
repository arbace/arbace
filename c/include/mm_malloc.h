/* Arbace's <mm_malloc.h> (c/README.md): Intel's aligned allocation, over POSIX's posix_memalign (musl). */
#ifndef __ARBACE_MM_MALLOC_H
#define __ARBACE_MM_MALLOC_H
#include <stdlib.h>
/* POSIX.1-2008's posix_memalign, declared here too: musl's <stdlib.h> declares it only under a POSIX or GNU feature test
   macro, and a program of strict ISO C (-std=c99: __STRICT_ANSI__) including <mm_malloc.h> through the intrinsics' headers
   still gets _mm_malloc (the own target's triage: benchmarksgame's six SSE programs, an implicit declaration otherwise). */
extern int posix_memalign(void **, __SIZE_TYPE__, __SIZE_TYPE__);
static __inline__ void *_mm_malloc(__SIZE_TYPE__ __size, __SIZE_TYPE__ __align) {
  void *__p;
  if (__align < sizeof(void *)) __align = sizeof(void *);
  return posix_memalign(&__p, __align, __size) == 0 ? __p : (void *)0;
}
static __inline__ void _mm_free(void *__p) { free(__p); }
#endif
