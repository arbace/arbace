/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* Review 053's finding 1: _Float16 and __bf16 are evaluated in float (FLT_EVAL_METHOD 0, gcc 15.2 on x86-64): a constant's value is the
   float nearest it, an operation is in float, and only a conversion (an initialization, a cast) rounds to the type's format. */
constexpr _Float16 a = 0.1f16;			/* { dg-error "not representable" } */
constexpr _Float16 b = 1.0f16/3;		/* { dg-error "not representable" } */
constexpr __bf16 c = 2049.0f16 - 1;
constexpr __bf16 d = 1.0f16 + 0.0001f16;	/* { dg-error "not representable" } */
constexpr _Float16 e = 2049.0f16;		/* { dg-error "not representable" } */
constexpr _Float16 f = 2048.0f16;
_Static_assert((int)2049.0f16 == 2049);
enum { E = (int)2049.0f16 };
_Static_assert(E == 2049);
constexpr __bf16 g = (_Float16)1.000488281250000931322574615478515625f16;
constexpr float h = 0.1f16;
constexpr float i = 0.1f16 * 3;
constexpr float j = (_Float16)0.1;
constexpr float k = (_Float16)0.1 + 0.1f16;
