/* Arbace's <immintrin.h> (c/README.md): Intel's SSE to AVX intrinsics as the Intel Intrinsics Guide names them, for x86_64. Arbace's C
   has no SIMD (doc/c/SPEC.md §14, by design): its vector types are GNU vector types, which ci refuses, and the functions
   are declared only, so that programs using them read and check; a call does not link. */
#ifndef __ARBACE_IMMINTRIN_H
#define __ARBACE_IMMINTRIN_H
#include <smmintrin.h>
typedef float __m256 __attribute__((__vector_size__(32), __may_alias__));
typedef double __m256d __attribute__((__vector_size__(32), __may_alias__));
typedef long long __m256i __attribute__((__vector_size__(32), __may_alias__));
extern __m256d _mm256_setzero_pd(void);
extern __m256d _mm256_set1_pd(double);
extern __m256d _mm256_set_pd(double, double, double, double);
extern __m256d _mm256_setr_pd(double, double, double, double);
extern __m256d _mm256_load_pd(double const *);
extern __m256d _mm256_loadu_pd(double const *);
extern void _mm256_store_pd(double *, __m256d);
extern void _mm256_storeu_pd(double *, __m256d);
extern __m256d _mm256_add_pd(__m256d, __m256d);
extern __m256d _mm256_sub_pd(__m256d, __m256d);
extern __m256d _mm256_mul_pd(__m256d, __m256d);
extern __m256d _mm256_div_pd(__m256d, __m256d);
extern __m256d _mm256_sqrt_pd(__m256d);
extern __m256d _mm256_hadd_pd(__m256d, __m256d);
extern __m256d _mm256_blend_pd(__m256d, __m256d, int);
extern __m256d _mm256_permute_pd(__m256d, int);
extern __m256d _mm256_permute2f128_pd(__m256d, __m256d, int);
extern __m256d _mm256_unpackhi_pd(__m256d, __m256d);
extern __m256d _mm256_unpacklo_pd(__m256d, __m256d);
extern __m128d _mm256_extractf128_pd(__m256d, int);
extern __m256d _mm256_castpd128_pd256(__m128d);
extern __m128d _mm256_castpd256_pd128(__m256d);
extern __m128 _mm256_cvtpd_ps(__m256d);
extern __m256d _mm256_cvtps_pd(__m128);
extern __m256d _mm256_cvtepi32_pd(__m128i);
extern __m256 _mm256_set1_ps(float);
extern __m256 _mm256_add_ps(__m256, __m256);
extern __m256 _mm256_mul_ps(__m256, __m256);
extern __m256 _mm256_rsqrt_ps(__m256);
extern void _mm256_zeroupper(void);
#endif
