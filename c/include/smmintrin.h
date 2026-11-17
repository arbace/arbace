/* Arbace's <smmintrin.h> (c/README.md): Intel's SSE4.1 intrinsics as the Intel Intrinsics Guide names them, for x86_64. Arbace's C
   has no SIMD (doc/c/SPEC.md §14, by design): its vector types are GNU vector types, which ci refuses, and the functions
   are declared only, so that programs using them read and check; a call does not link. */
#ifndef __ARBACE_SMMINTRIN_H
#define __ARBACE_SMMINTRIN_H
#include <tmmintrin.h>
extern __m128i _mm_mullo_epi32(__m128i, __m128i);
extern __m128d _mm_blend_pd(__m128d, __m128d, int);
extern __m128d _mm_round_pd(__m128d, int);
extern int _mm_extract_epi32(__m128i, int);
#endif
