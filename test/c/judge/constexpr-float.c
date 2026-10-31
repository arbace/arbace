/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* Review 027's finding 1: float op float is evaluated in float, so these initializers are exact (gcc 15 accepts them). */
constexpr float a = 0.1f + 0.2f;
constexpr float b = 1.0f / 3.0f;
constexpr float c = 0.1f * 3;
constexpr float g = (float)1 / 3;
constexpr _Complex float z = __builtin_complex (1.0f / 3, 0.0f);
constexpr float h = 0.1f - 0.3f;
/* in double, not exact in float */
constexpr float d = 0.1f + 0.2;			/* { dg-error "not representable" } */
constexpr float e = 1.0f / 3.0;			/* { dg-error "not representable" } */
