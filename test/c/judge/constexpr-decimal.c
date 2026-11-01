/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* CS215: a decimal constant of the object's own type is judged by its value rounded to the format (gcc 15). */
constexpr _Decimal32 a = 1.000000000DF;
constexpr _Decimal32 b = 1.0000001DF;
constexpr _Decimal32 c = 1.2345678DF;
constexpr _Decimal32 d = 1.0DD;
constexpr _Decimal32 e = 1.0000001DD;		/* { dg-error "not representable" } */
constexpr _Decimal64 f = 1.0000001DF;
constexpr _Decimal32 m = 1.000000000DL;		/* { dg-error "not representable" } */
constexpr _Decimal64 l = 1.0000000000000000000DD;
/* review 031's finding D: a _Decimal32 constant is its value rounded to 7 digits, which _Decimal64 and _Decimal128 hold (gcc 15, x86-64) */
constexpr _Decimal64 n = 1.00000000000000000DF;
constexpr _Decimal128 o = 1.00000000000000000DF;
constexpr _Decimal64 q = 1.2345678901234567890DF;
constexpr _Decimal64 r = 1.00000000000000000000DL;	/* { dg-error "not representable" } */
constexpr _Decimal32 s = 1.000000000DD;		/* { dg-error "not representable" } */
