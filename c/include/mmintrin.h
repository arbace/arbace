/* Arbace's <mmintrin.h> (c/README.md): Intel's MMX intrinsics as the Intel Intrinsics Guide names them, for x86_64. Arbace's C
   has no SIMD (doc/c/SPEC.md §14, by design): its vector types are GNU vector types, which ci refuses, and the functions
   are declared only, so that programs using them read and check; a call does not link. */
#ifndef __ARBACE_MMINTRIN_H
#define __ARBACE_MMINTRIN_H
typedef int __m64 __attribute__((__vector_size__(8), __may_alias__));
extern void _mm_empty(void);
#endif
