/* { dg-do compile { target { x86_64-*-* } } } */
/* { dg-options "-std=c23" } */
/* CS235 (a): a long double constant keeps its exact value through unary + and - and a cast to long double. */
constexpr double a = -0.1L;			/* { dg-error "not representable" } */
constexpr double b = +0.1L;			/* { dg-error "not representable" } */
constexpr double c = (long double)0.1L;		/* { dg-error "not representable" } */
constexpr double d = (long double)0.1;
constexpr double e = -0.5L;
constexpr double f = (long double)3;
constexpr double g = - -0.1L;			/* { dg-error "not representable" } */
