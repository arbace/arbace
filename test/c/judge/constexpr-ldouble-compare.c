/* { dg-do compile { target { x86_64-*-* } } } */
/* { dg-options "-std=c23" } */
/* Review 057's finding 4: a long double constant is compared by its exact value, not its double. */
_Static_assert(0.1L != 0.1);
_Static_assert(0.1L > 0.1 || 0.1L < 0.1);
_Static_assert(0.1L == 0.1L);
_Static_assert(0.5L == 0.5);
_Static_assert(-0.1L != -0.1);
_Static_assert(0.1L != 0.1f);
/* review 079's finding 1: an operand with no exact value (arithmetic, ?:, an identifier) is compared as before, by its double */
_Static_assert(0.1L == 0.1L * 1);
_Static_assert(0.1L == 0.1L + 0);
_Static_assert(0.1L == (1 ? 0.1L : 0));
constexpr long double x = 0.1L;
_Static_assert(x == 0.1L);
_Static_assert(1e-7000L == 0);
