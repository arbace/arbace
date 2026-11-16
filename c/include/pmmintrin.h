/* Arbace's <pmmintrin.h> (c/README.md): Intel's SSE3 intrinsics as the Intel Intrinsics Guide names them, for x86_64. Arbace's C
   has no SIMD (doc/c/SPEC.md §14, by design): its vector types are GNU vector types, which ci refuses, and the functions
   are declared only, so that programs using them read and check; a call does not link. */
#ifndef __ARBACE_PMMINTRIN_H
#define __ARBACE_PMMINTRIN_H
#include <emmintrin.h>
extern __m128d _mm_hadd_pd(__m128d, __m128d);
extern __m128 _mm_hadd_ps(__m128, __m128);
extern __m128d _mm_movedup_pd(__m128d);
#endif
