/* { dg-do compile { target { x86_64-*-* } } } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* Review 110's finding 2: a constexpr object holds a _Float128 constant only when the format keeps its value: binary128's
   0.1 is neither a double nor (on x86-64) a long double; an integer beyond binary128's 113 bits is not a _Float128. */
constexpr double a = 0.1F128;		/* { dg-error "not representable" } */
constexpr double b = 0.5F128;
constexpr long double c = 0.1F128;	/* { dg-error "not representable" } */
constexpr long double d = 0.5F128;
constexpr _Float128 e = 0.1L;
constexpr _Float128 f = 0.1;
constexpr _Float128 g = ((unsigned _BitInt(128))1 << 120) + 1;	/* { dg-error "not representable" } */
constexpr _Float128 h = ((unsigned _BitInt(128))1 << 112) + 1;
constexpr long double i = ((unsigned _BitInt(128))1 << 64) + 1;	/* { dg-error "not representable" } */
constexpr _Float64x j = 0.1F128;	/* { dg-error "not representable" } */
double k = 1.0q;			/* { dg-error "non-standard suffix on floating constant" } */
