/* ci-run verdict: exit 0. gcc's float and long double spellings of the classification builtins: __builtin_isinff, isinfl, isnanf, isnanl,
   finitef, finitel (gcc's __builtin_isinf etc. are type-generic; these take their own type). Queue 126
   (gcc.c-torture/execute/pr39228.c). Long double values a double holds alike. Checks itself: a classification other than
   IEEE 754's returns 1. */
#include <stdio.h>
int main(void) {
  volatile float fm = __FLT_MAX__, fz = 0.0f; volatile double dm = __DBL_MAX__; volatile long double lz = 0.0L, lm = 1e300L;
  float fi = fm * 2.0f, fn = fz / fz; long double li = 1.0L / lz, ln = li - li; /* no LDBL_MAX: long double is double in ci (CS35) */
  printf("%d %d %d %d\n", __builtin_isinff(fi), __builtin_isinff(-fi), __builtin_isinff(fm), __builtin_isinff(fn));
  printf("%d %d %d %d\n", __builtin_isinfl(li), __builtin_isinfl(-li), __builtin_isinfl(lm), __builtin_isinfl(ln));
  printf("%d %d %d %d\n", __builtin_isnanf(fn), __builtin_isnanf(fi), __builtin_isnanl(ln), __builtin_isnanl(lm));
  printf("%d %d %d %d\n", __builtin_finitef(fm), __builtin_finitef(fi), __builtin_finitel(lm), __builtin_finitel(ln));
  printf("%d %d\n", __builtin_isinff(1.01f * fm) != 0, __builtin_isinf(1.01 * dm) != 0);
  enum { E1 = __builtin_isinff(__builtin_inff()), E2 = __builtin_isnanl(0.0L) };
  printf("%d %d\n", E1, E2);
  int got[] = {!!__builtin_isinff(fi), !!__builtin_isinff(-fi), !!__builtin_isinff(fm), !!__builtin_isinff(fn),
               !!__builtin_isinfl(li), !!__builtin_isinfl(-li), !!__builtin_isinfl(lm), !!__builtin_isinfl(ln),
               !!__builtin_isnanf(fn), !!__builtin_isnanf(fi), !!__builtin_isnanl(ln), !!__builtin_isnanl(lm),
               !!__builtin_finitef(fm), !!__builtin_finitef(fi), !!__builtin_finitel(lm), !!__builtin_finitel(ln),
               __builtin_isinff(1.01f * fm) != 0, __builtin_isinf(1.01 * dm) != 0, !!E1, !!E2};
  static const int want[] = {1, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0};
  for (int i = 0; i < 20; i++) if (got[i] != want[i]) return 1;
  return 0;
}
