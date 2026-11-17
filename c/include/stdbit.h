/* Arbace's <stdbit.h> (ISO/IEC 9899:2024 §7.18), c/README.md: the endian macros, the type-generic bit utilities over the
   GNU dialect's __builtin_stdc_ builtins, and the functions of each unsigned standard type as inline definitions over
   them (musl 1.2.6 has no <stdbit.h>). */
#ifndef __ARBACE_STDBIT_H
#define __ARBACE_STDBIT_H
#define __STDC_VERSION_STDBIT_H__ 202311L
#define __STDC_ENDIAN_LITTLE__ __ORDER_LITTLE_ENDIAN__
#define __STDC_ENDIAN_BIG__ __ORDER_BIG_ENDIAN__
#define __STDC_ENDIAN_NATIVE__ __BYTE_ORDER__
#define stdc_leading_zeros(value) __builtin_stdc_leading_zeros(value)
static __inline__ unsigned int stdc_leading_zeros_uc(unsigned char __x) { return __builtin_stdc_leading_zeros(__x); }
static __inline__ unsigned int stdc_leading_zeros_us(unsigned short __x) { return __builtin_stdc_leading_zeros(__x); }
static __inline__ unsigned int stdc_leading_zeros_ui(unsigned int __x) { return __builtin_stdc_leading_zeros(__x); }
static __inline__ unsigned int stdc_leading_zeros_ul(unsigned long __x) { return __builtin_stdc_leading_zeros(__x); }
static __inline__ unsigned int stdc_leading_zeros_ull(unsigned long long __x) { return __builtin_stdc_leading_zeros(__x); }
#define stdc_leading_ones(value) __builtin_stdc_leading_ones(value)
static __inline__ unsigned int stdc_leading_ones_uc(unsigned char __x) { return __builtin_stdc_leading_ones(__x); }
static __inline__ unsigned int stdc_leading_ones_us(unsigned short __x) { return __builtin_stdc_leading_ones(__x); }
static __inline__ unsigned int stdc_leading_ones_ui(unsigned int __x) { return __builtin_stdc_leading_ones(__x); }
static __inline__ unsigned int stdc_leading_ones_ul(unsigned long __x) { return __builtin_stdc_leading_ones(__x); }
static __inline__ unsigned int stdc_leading_ones_ull(unsigned long long __x) { return __builtin_stdc_leading_ones(__x); }
#define stdc_trailing_zeros(value) __builtin_stdc_trailing_zeros(value)
static __inline__ unsigned int stdc_trailing_zeros_uc(unsigned char __x) { return __builtin_stdc_trailing_zeros(__x); }
static __inline__ unsigned int stdc_trailing_zeros_us(unsigned short __x) { return __builtin_stdc_trailing_zeros(__x); }
static __inline__ unsigned int stdc_trailing_zeros_ui(unsigned int __x) { return __builtin_stdc_trailing_zeros(__x); }
static __inline__ unsigned int stdc_trailing_zeros_ul(unsigned long __x) { return __builtin_stdc_trailing_zeros(__x); }
static __inline__ unsigned int stdc_trailing_zeros_ull(unsigned long long __x) { return __builtin_stdc_trailing_zeros(__x); }
#define stdc_trailing_ones(value) __builtin_stdc_trailing_ones(value)
static __inline__ unsigned int stdc_trailing_ones_uc(unsigned char __x) { return __builtin_stdc_trailing_ones(__x); }
static __inline__ unsigned int stdc_trailing_ones_us(unsigned short __x) { return __builtin_stdc_trailing_ones(__x); }
static __inline__ unsigned int stdc_trailing_ones_ui(unsigned int __x) { return __builtin_stdc_trailing_ones(__x); }
static __inline__ unsigned int stdc_trailing_ones_ul(unsigned long __x) { return __builtin_stdc_trailing_ones(__x); }
static __inline__ unsigned int stdc_trailing_ones_ull(unsigned long long __x) { return __builtin_stdc_trailing_ones(__x); }
#define stdc_first_leading_zero(value) __builtin_stdc_first_leading_zero(value)
static __inline__ unsigned int stdc_first_leading_zero_uc(unsigned char __x) { return __builtin_stdc_first_leading_zero(__x); }
static __inline__ unsigned int stdc_first_leading_zero_us(unsigned short __x) { return __builtin_stdc_first_leading_zero(__x); }
static __inline__ unsigned int stdc_first_leading_zero_ui(unsigned int __x) { return __builtin_stdc_first_leading_zero(__x); }
static __inline__ unsigned int stdc_first_leading_zero_ul(unsigned long __x) { return __builtin_stdc_first_leading_zero(__x); }
static __inline__ unsigned int stdc_first_leading_zero_ull(unsigned long long __x) { return __builtin_stdc_first_leading_zero(__x); }
#define stdc_first_leading_one(value) __builtin_stdc_first_leading_one(value)
static __inline__ unsigned int stdc_first_leading_one_uc(unsigned char __x) { return __builtin_stdc_first_leading_one(__x); }
static __inline__ unsigned int stdc_first_leading_one_us(unsigned short __x) { return __builtin_stdc_first_leading_one(__x); }
static __inline__ unsigned int stdc_first_leading_one_ui(unsigned int __x) { return __builtin_stdc_first_leading_one(__x); }
static __inline__ unsigned int stdc_first_leading_one_ul(unsigned long __x) { return __builtin_stdc_first_leading_one(__x); }
static __inline__ unsigned int stdc_first_leading_one_ull(unsigned long long __x) { return __builtin_stdc_first_leading_one(__x); }
#define stdc_first_trailing_zero(value) __builtin_stdc_first_trailing_zero(value)
static __inline__ unsigned int stdc_first_trailing_zero_uc(unsigned char __x) { return __builtin_stdc_first_trailing_zero(__x); }
static __inline__ unsigned int stdc_first_trailing_zero_us(unsigned short __x) { return __builtin_stdc_first_trailing_zero(__x); }
static __inline__ unsigned int stdc_first_trailing_zero_ui(unsigned int __x) { return __builtin_stdc_first_trailing_zero(__x); }
static __inline__ unsigned int stdc_first_trailing_zero_ul(unsigned long __x) { return __builtin_stdc_first_trailing_zero(__x); }
static __inline__ unsigned int stdc_first_trailing_zero_ull(unsigned long long __x) { return __builtin_stdc_first_trailing_zero(__x); }
#define stdc_first_trailing_one(value) __builtin_stdc_first_trailing_one(value)
static __inline__ unsigned int stdc_first_trailing_one_uc(unsigned char __x) { return __builtin_stdc_first_trailing_one(__x); }
static __inline__ unsigned int stdc_first_trailing_one_us(unsigned short __x) { return __builtin_stdc_first_trailing_one(__x); }
static __inline__ unsigned int stdc_first_trailing_one_ui(unsigned int __x) { return __builtin_stdc_first_trailing_one(__x); }
static __inline__ unsigned int stdc_first_trailing_one_ul(unsigned long __x) { return __builtin_stdc_first_trailing_one(__x); }
static __inline__ unsigned int stdc_first_trailing_one_ull(unsigned long long __x) { return __builtin_stdc_first_trailing_one(__x); }
#define stdc_count_zeros(value) __builtin_stdc_count_zeros(value)
static __inline__ unsigned int stdc_count_zeros_uc(unsigned char __x) { return __builtin_stdc_count_zeros(__x); }
static __inline__ unsigned int stdc_count_zeros_us(unsigned short __x) { return __builtin_stdc_count_zeros(__x); }
static __inline__ unsigned int stdc_count_zeros_ui(unsigned int __x) { return __builtin_stdc_count_zeros(__x); }
static __inline__ unsigned int stdc_count_zeros_ul(unsigned long __x) { return __builtin_stdc_count_zeros(__x); }
static __inline__ unsigned int stdc_count_zeros_ull(unsigned long long __x) { return __builtin_stdc_count_zeros(__x); }
#define stdc_count_ones(value) __builtin_stdc_count_ones(value)
static __inline__ unsigned int stdc_count_ones_uc(unsigned char __x) { return __builtin_stdc_count_ones(__x); }
static __inline__ unsigned int stdc_count_ones_us(unsigned short __x) { return __builtin_stdc_count_ones(__x); }
static __inline__ unsigned int stdc_count_ones_ui(unsigned int __x) { return __builtin_stdc_count_ones(__x); }
static __inline__ unsigned int stdc_count_ones_ul(unsigned long __x) { return __builtin_stdc_count_ones(__x); }
static __inline__ unsigned int stdc_count_ones_ull(unsigned long long __x) { return __builtin_stdc_count_ones(__x); }
#define stdc_has_single_bit(value) __builtin_stdc_has_single_bit(value)
static __inline__ _Bool stdc_has_single_bit_uc(unsigned char __x) { return __builtin_stdc_has_single_bit(__x); }
static __inline__ _Bool stdc_has_single_bit_us(unsigned short __x) { return __builtin_stdc_has_single_bit(__x); }
static __inline__ _Bool stdc_has_single_bit_ui(unsigned int __x) { return __builtin_stdc_has_single_bit(__x); }
static __inline__ _Bool stdc_has_single_bit_ul(unsigned long __x) { return __builtin_stdc_has_single_bit(__x); }
static __inline__ _Bool stdc_has_single_bit_ull(unsigned long long __x) { return __builtin_stdc_has_single_bit(__x); }
#define stdc_bit_width(value) __builtin_stdc_bit_width(value)
static __inline__ unsigned int stdc_bit_width_uc(unsigned char __x) { return __builtin_stdc_bit_width(__x); }
static __inline__ unsigned int stdc_bit_width_us(unsigned short __x) { return __builtin_stdc_bit_width(__x); }
static __inline__ unsigned int stdc_bit_width_ui(unsigned int __x) { return __builtin_stdc_bit_width(__x); }
static __inline__ unsigned int stdc_bit_width_ul(unsigned long __x) { return __builtin_stdc_bit_width(__x); }
static __inline__ unsigned int stdc_bit_width_ull(unsigned long long __x) { return __builtin_stdc_bit_width(__x); }
#define stdc_bit_floor(value) __builtin_stdc_bit_floor(value)
static __inline__ unsigned char stdc_bit_floor_uc(unsigned char __x) { return __builtin_stdc_bit_floor(__x); }
static __inline__ unsigned short stdc_bit_floor_us(unsigned short __x) { return __builtin_stdc_bit_floor(__x); }
static __inline__ unsigned int stdc_bit_floor_ui(unsigned int __x) { return __builtin_stdc_bit_floor(__x); }
static __inline__ unsigned long stdc_bit_floor_ul(unsigned long __x) { return __builtin_stdc_bit_floor(__x); }
static __inline__ unsigned long long stdc_bit_floor_ull(unsigned long long __x) { return __builtin_stdc_bit_floor(__x); }
#define stdc_bit_ceil(value) __builtin_stdc_bit_ceil(value)
static __inline__ unsigned char stdc_bit_ceil_uc(unsigned char __x) { return __builtin_stdc_bit_ceil(__x); }
static __inline__ unsigned short stdc_bit_ceil_us(unsigned short __x) { return __builtin_stdc_bit_ceil(__x); }
static __inline__ unsigned int stdc_bit_ceil_ui(unsigned int __x) { return __builtin_stdc_bit_ceil(__x); }
static __inline__ unsigned long stdc_bit_ceil_ul(unsigned long __x) { return __builtin_stdc_bit_ceil(__x); }
static __inline__ unsigned long long stdc_bit_ceil_ull(unsigned long long __x) { return __builtin_stdc_bit_ceil(__x); }
#endif
