// synth-c23/lib-math-builtins.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// <math.h>'s functions called by name and in gcc's __builtin_ spellings, double, float and long double
// ISO/IEC 9899:2024 7.12; Annex A: (library)
#include <stdio.h>
#include <math.h>

int main(void) {
  printf("%s = %g\n", "log2(2.75)", log2(2.75));
  printf("%s = %g\n", "__builtin_log2(2.75)", __builtin_log2(2.75));
  printf("%s = %g\n", "exp2(3.5)", exp2(3.5));
  printf("%s = %g\n", "__builtin_exp2(3.5)", __builtin_exp2(3.5));
  printf("%s = %g\n", "(double)__builtin_log2f(8.0f)", (double)__builtin_log2f(8.0f));
  printf("%s = %g\n", "(double)__builtin_exp2f(3.5f)", (double)__builtin_exp2f(3.5f));
  printf("%s = %g\n", "(double)__builtin_log2l(16.0L)", (double)__builtin_log2l(16.0L));
  printf("%s = %g\n", "__builtin_trunc(-2.5)", __builtin_trunc(-2.5));
  printf("%s = %g\n", "__builtin_round(2.5)", __builtin_round(2.5));
  printf("%s = %g\n", "__builtin_rint(2.5)", __builtin_rint(2.5));
  printf("%s = %g\n", "__builtin_nearbyint(3.5)", __builtin_nearbyint(3.5));
  printf("%s = %g\n", "__builtin_cbrt(27.0)", __builtin_cbrt(27.0));
  printf("%s = %g\n", "__builtin_fmin(1.5, -2.0)", __builtin_fmin(1.5, -2.0));
  printf("%s = %g\n", "__builtin_fmax(1.5, -2.0)", __builtin_fmax(1.5, -2.0));
  printf("%s = %g\n", "__builtin_hypot(3.0, 4.0)", __builtin_hypot(3.0, 4.0));
  printf("%s = %g\n", "__builtin_fdim(5.0, 2.0)", __builtin_fdim(5.0, 2.0));
  printf("%s = %g\n", "__builtin_fma(2.0, 3.0, 1.0)", __builtin_fma(2.0, 3.0, 1.0));
  printf("%s = %g\n", "(double)__builtin_fmaf(2.0f, 3.0f, 0.5f)", (double)__builtin_fmaf(2.0f, 3.0f, 0.5f));
  printf("%s = %d\n", "__builtin_ilogb(40.0)", __builtin_ilogb(40.0));
  printf("%s = %ld\n", "__builtin_lround(2.5)", __builtin_lround(2.5));
  printf("%s = %lld\n", "__builtin_llrint(-3.5)", __builtin_llrint(-3.5));
  printf("%s = %g\n", "__builtin_scalbn(1.5, 3)", __builtin_scalbn(1.5, 3));
  printf("%s = %g\n", "__builtin_remainder(7.0, 2.0)", __builtin_remainder(7.0, 2.0));
  printf("%s = %g\n", "__builtin_logb(40.0)", __builtin_logb(40.0));
  printf("%s = %g\n", "fmin(2.0, 3.0) + fmax(2.0, 3.0)", fmin(2.0, 3.0) + fmax(2.0, 3.0));
  return 0;
}
