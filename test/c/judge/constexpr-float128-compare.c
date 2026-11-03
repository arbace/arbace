/* { dg-do compile { target { x86_64-*-* } } } */
/* { dg-options "-std=gnu23" } */
/* Review 110's finding 2: a _Float128 or _Float64x constant keeps its exact value, and a comparison in a type wider than
   double (long double and _Float64x: x87's 64-bit significand; _Float128: binary128's 113 bits) compares the operands
   converted to that type exactly. On x86-64 (clang --target=x86_64-linux-gnu, with __float128 and the q suffix) all hold;
   on aarch64, where long double is binary128, the lines comparing 0.1L with 0.1F128 do not. */
_Static_assert(0.1L != (_Float128)0.1);
_Static_assert(0.1F128 != 0.1);
_Static_assert(0.1F128 != 0.1L);
_Static_assert((_Float128)0.1L != 0.1F128);
_Static_assert(0.1F128 == 0.1F128);
_Static_assert(0.1F128 < 0.1);
_Static_assert(0.1L > 0.1F128);
_Static_assert(-0.1F128 > -0.1);
_Static_assert(-0.1F128 != -0.1L);
_Static_assert((_Float128)(double)0.1F128 == 0.1);
_Static_assert((long double)0.1F128 != 0.1F128);
_Static_assert((long double)0.1F128 == 0.1L);
_Static_assert((_Float128)(long double)0.1F128 == 0.1L);
/* beyond double's range: subnormal and huge binary128 values */
_Static_assert(1e-4940F128 > 0);
_Static_assert(0x1p-16382F128 > 0);
_Static_assert(1e400F128 != 2e400F128);
_Static_assert(1e400F128 < 2e400F128);
/* an integer converted to the comparison's type is rounded to its significand */
_Static_assert(((unsigned _BitInt(128))1 << 120) + 1 == 0x1p120F128);
_Static_assert(((unsigned _BitInt(128))1 << 100) + 1 != 0x1p100F128);
_Static_assert(((unsigned _BitInt(128))1 << 120) + 1 == 0x1p120L);
/* _Float64x is x87's format on x86-64 */
_Static_assert(0.1f64x == 0.1L);
_Static_assert(0.1f64x != 0.1);
/* a cast of a wide value with no exact value gives none (review 079's finding 1a) */
_Static_assert(0.1L == (long double)(0.1L * 1));
/* GNU's names: the q suffix is __float128, _Float128 in C */
_Static_assert(0.1q != 0.1L);
_Static_assert(0.1q == 0.1F128);
_Static_assert((__float128)0.1L == 0.1L);
