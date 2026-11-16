/* Arbace's <tmmintrin.h> (c/README.md): Intel's SSSE3 intrinsics as the Intel Intrinsics Guide names them, for x86_64. Arbace's C
   has no SIMD (doc/c/SPEC.md §14, by design): its vector types are GNU vector types, which ci refuses, and the functions
   are declared only, so that programs using them read and check; a call does not link. */
#ifndef __ARBACE_TMMINTRIN_H
#define __ARBACE_TMMINTRIN_H
#include <pmmintrin.h>
extern __m128i _mm_shuffle_epi8(__m128i, __m128i);
extern __m128i _mm_alignr_epi8(__m128i, __m128i, int);
extern __m128i _mm_abs_epi32(__m128i);
#endif
