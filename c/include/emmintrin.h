/* Arbace's <emmintrin.h> (c/README.md): Intel's SSE2 intrinsics as the Intel Intrinsics Guide names them, for x86_64. Arbace's C
   has no SIMD (doc/c/SPEC.md §14, by design): its vector types are GNU vector types, which ci refuses, and the functions
   are declared only, so that programs using them read and check; a call does not link. */
#ifndef __ARBACE_EMMINTRIN_H
#define __ARBACE_EMMINTRIN_H
#include <xmmintrin.h>
typedef double __m128d __attribute__((__vector_size__(16), __may_alias__));
typedef long long __m128i __attribute__((__vector_size__(16), __may_alias__));
typedef double __m128d_u __attribute__((__vector_size__(16), __may_alias__, __aligned__(1)));
typedef long long __m128i_u __attribute__((__vector_size__(16), __may_alias__, __aligned__(1)));
extern __m128d _mm_setzero_pd(void);
extern __m128d _mm_set1_pd(double);
extern __m128d _mm_set_pd(double, double);
extern __m128d _mm_setr_pd(double, double);
extern __m128d _mm_load_pd(double const *);
extern __m128d _mm_loadu_pd(double const *);
extern __m128d _mm_loadh_pd(__m128d, double const *);
extern __m128d _mm_loadl_pd(__m128d, double const *);
extern void _mm_store_pd(double *, __m128d);
extern void _mm_storeu_pd(double *, __m128d);
extern void _mm_storeh_pd(double *, __m128d);
extern void _mm_storel_pd(double *, __m128d);
extern __m128d _mm_add_pd(__m128d, __m128d);
extern __m128d _mm_sub_pd(__m128d, __m128d);
extern __m128d _mm_mul_pd(__m128d, __m128d);
extern __m128d _mm_div_pd(__m128d, __m128d);
extern __m128d _mm_sqrt_pd(__m128d);
extern __m128d _mm_min_pd(__m128d, __m128d);
extern __m128d _mm_max_pd(__m128d, __m128d);
extern __m128d _mm_unpackhi_pd(__m128d, __m128d);
extern __m128d _mm_unpacklo_pd(__m128d, __m128d);
extern double _mm_cvtsd_f64(__m128d);
extern __m128 _mm_cvtpd_ps(__m128d);
extern __m128d _mm_cvtps_pd(__m128);
extern __m128i _mm_setzero_si128(void);
extern __m128i _mm_set1_epi8(char);
extern __m128i _mm_set1_epi16(short);
extern __m128i _mm_set1_epi32(int);
extern __m128i _mm_set1_epi64x(long long);
extern __m128i _mm_set_epi32(int, int, int, int);
extern __m128i _mm_setr_epi32(int, int, int, int);
extern __m128i _mm_load_si128(__m128i const *);
extern __m128i _mm_loadu_si128(__m128i_u const *);
extern __m128i _mm_loadl_epi64(__m128i_u const *);
extern void _mm_store_si128(__m128i *, __m128i);
extern void _mm_storeu_si128(__m128i_u *, __m128i);
extern void _mm_storel_epi64(__m128i_u *, __m128i);
extern __m128i _mm_add_epi8(__m128i, __m128i);
extern __m128i _mm_add_epi16(__m128i, __m128i);
extern __m128i _mm_add_epi32(__m128i, __m128i);
extern __m128i _mm_add_epi64(__m128i, __m128i);
extern __m128i _mm_sub_epi32(__m128i, __m128i);
extern __m128i _mm_and_si128(__m128i, __m128i);
extern __m128i _mm_or_si128(__m128i, __m128i);
extern __m128i _mm_xor_si128(__m128i, __m128i);
extern __m128i _mm_slli_epi32(__m128i, int);
extern __m128i _mm_srli_epi32(__m128i, int);
extern __m128i _mm_slli_si128(__m128i, int);
extern __m128i _mm_srli_si128(__m128i, int);
extern __m128i _mm_unpackhi_epi64(__m128i, __m128i);
extern __m128i _mm_unpacklo_epi64(__m128i, __m128i);
extern __m128i _mm_cmpeq_epi8(__m128i, __m128i);
extern int _mm_movemask_epi8(__m128i);
extern int _mm_cvtsi128_si32(__m128i);
extern __m128i _mm_cvtsi32_si128(int);
extern __m128d _mm_castsi128_pd(__m128i);
extern __m128i _mm_castpd_si128(__m128d);
#endif
