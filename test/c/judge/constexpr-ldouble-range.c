/* { dg-do compile { target { x86_64-*-* } } } */
/* { dg-options "-std=c23" } */
/* Review 053's finding 4: a long double constant beyond double's range (or float's) is not representable in the object; the binary
   exponent is exact however small or large the constant. */
constexpr double a = 1e400L;			/* { dg-error "not representable" } */
constexpr double b = 1e-400L;			/* { dg-error "not representable" } */
constexpr double c = 4.9406564584124654e-324L;	/* { dg-error "not representable" } */
constexpr float d = 0.5000000000000000001L;	/* { dg-error "not representable" } */
constexpr float e = 1e-50L;			/* { dg-error "not representable" } */
constexpr float f = 0.5L;
constexpr _Float16 g = 0.1L;			/* { dg-error "not representable" } */
constexpr float h = 1e39L;			/* { dg-error "not representable" } */
constexpr double i = 0.5L;
