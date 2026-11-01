/* { dg-do compile { target { x86_64-*-* && int128 } } } */
/* { dg-options "-std=c23" } */
/* Review 027's finding 8: x87's long double holds an integer of up to 64 significant bits, however wide; a signaling NaN
   stays one under unary + and -. */
constexpr long double e = (unsigned __int128)1 << 100;
constexpr long double f = ((__int128)3) << 80;
constexpr long double g = -(((__int128)3) << 80);
constexpr long double h = ((unsigned __int128)1 << 65) + 1;	/* { dg-error "not representable" } */
constexpr double z = -__builtin_nansf ("");			/* { dg-error "not representable" } */
constexpr double y = +__builtin_nansf ("");			/* { dg-error "not representable" } */
constexpr float w = -__builtin_nansf ("");
constexpr double v = -__builtin_nans ("");
