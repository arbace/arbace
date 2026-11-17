/* Arbace's <stdckdint.h> (ISO/IEC 9899:2024 §7.20), c/README.md: the checked integer operations, over the GNU
   dialect's overflow builtins (the result's type any integer type but bool, char, an enumeration or a bit-precise one). */
#ifndef __ARBACE_STDCKDINT_H
#define __ARBACE_STDCKDINT_H
#define __STDC_VERSION_STDCKDINT_H__ 202311L
#define ckd_add(result, a, b) __builtin_add_overflow((a), (b), (result))
#define ckd_sub(result, a, b) __builtin_sub_overflow((a), (b), (result))
#define ckd_mul(result, a, b) __builtin_mul_overflow((a), (b), (result))
#endif
