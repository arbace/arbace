/* oi's C API (queue 547): the part of SSE2's <emmintrin.h> a stub of the opam set uses (ocaml_intrinsics_kernel's float_stubs.c:
   _mm_set_sd, _mm_min_sd, _mm_max_sd, _mm_cvtsd_f64), Arbace's own, clean-room from the instructions' definitions in Intel's
   manual: minsd and maxsd give the second operand when the first is not less (greater), so when either is a NaN or both are
   zeros. The target (c/target) predefines __SSE2__, and Arbace's sysroot has no intrinsics header; this one is on the stubs'
   include path only. A vector is a struct of its two lanes. */
#ifndef ARBACE_OI_EMMINTRIN_H
#define ARBACE_OI_EMMINTRIN_H
typedef struct { double lane[2]; } __m128d;
static inline __m128d _mm_set_sd(double x) { __m128d r; r.lane[0] = x; r.lane[1] = 0.0; return r; }
static inline double _mm_cvtsd_f64(__m128d a) { return a.lane[0]; }
static inline __m128d _mm_min_sd(__m128d a, __m128d b) { __m128d r = a; r.lane[0] = a.lane[0] < b.lane[0] ? a.lane[0] : b.lane[0]; return r; }
static inline __m128d _mm_max_sd(__m128d a, __m128d b) { __m128d r = a; r.lane[0] = a.lane[0] > b.lane[0] ? a.lane[0] : b.lane[0]; return r; }
#endif
