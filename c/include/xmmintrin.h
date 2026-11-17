/* Arbace's <xmmintrin.h> (c/README.md): Intel's SSE intrinsics as the Intel Intrinsics Guide names them, for x86_64. Arbace's C
   has no SIMD (doc/c/SPEC.md §14, by design): its vector types are GNU vector types, which ci refuses, and the functions
   are declared only, so that programs using them read and check; a call does not link. */
#ifndef __ARBACE_XMMINTRIN_H
#define __ARBACE_XMMINTRIN_H
#include <mmintrin.h>
typedef float __m128 __attribute__((__vector_size__(16), __may_alias__));
typedef float __m128_u __attribute__((__vector_size__(16), __may_alias__, __aligned__(1)));
extern __m128 _mm_setzero_ps(void);
extern __m128 _mm_set1_ps(float);
extern __m128 _mm_set_ps(float, float, float, float);
extern __m128 _mm_setr_ps(float, float, float, float);
extern __m128 _mm_load_ps(float const *);
extern __m128 _mm_loadu_ps(float const *);
extern void _mm_store_ps(float *, __m128);
extern void _mm_storeu_ps(float *, __m128);
extern __m128 _mm_add_ps(__m128, __m128);
extern __m128 _mm_sub_ps(__m128, __m128);
extern __m128 _mm_mul_ps(__m128, __m128);
extern __m128 _mm_div_ps(__m128, __m128);
extern __m128 _mm_sqrt_ps(__m128);
extern __m128 _mm_rcp_ps(__m128);
extern __m128 _mm_rsqrt_ps(__m128);
extern __m128 _mm_min_ps(__m128, __m128);
extern __m128 _mm_max_ps(__m128, __m128);
extern float _mm_cvtss_f32(__m128);
extern void _mm_prefetch(char const *, int);
#define _MM_HINT_T0 3
#define _MM_HINT_T1 2
#define _MM_HINT_T2 1
#define _MM_HINT_NTA 0
#define _MM_SHUFFLE(z, y, x, w) (((z) << 6) | ((y) << 4) | ((x) << 2) | (w))
#include <mm_malloc.h>
#endif
