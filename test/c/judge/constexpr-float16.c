/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* Review 031's finding A: a _Float16 or __bf16 value is rounded to its format, so a float or double object holds it exactly
   (gcc 15 and clang accept these); a long double constant with a fraction is not a double. */
constexpr float a = 0.1f16;
constexpr float b = (_Float16)0.1;
constexpr double c = 0.1f16;
constexpr float d = (__bf16)0.1;
constexpr float e = 0.1f16 * 3;
constexpr _Float16 f = 0.5f16;
constexpr _Float16 k = 2048;
constexpr __bf16 l = 256;
constexpr _Float16 m = 0.1f;			/* { dg-error "not representable" } */
constexpr _Float16 n = 2049;			/* { dg-error "not representable" } */
constexpr _Float16 o = 65520;			/* { dg-error "not representable" } */
constexpr __bf16 q = 257;			/* { dg-error "not representable" } */
constexpr _Float16 r = 1e-8;			/* { dg-error "not representable" } */
constexpr double g = 0.5L;
constexpr float h = 0.1f;
constexpr float i = 0.1;			/* { dg-error "not representable" } */
constexpr double j = 0.1L;			/* { dg-error "not representable" } */
