/* queue 178 (CX8's finding 3): <math.h>'s functions in their __builtin_ spellings that cc's Builtin does not declare:
   an undeclared builtin returned int and took unpromoted arguments, so __builtin_log2(2.75) was 0, and the float
   spellings were undefined functions. No header: the builtins alone, every prototype shape and spelling.
   Self-checking: status 0 when ci agrees with gcc 15.2, 1..N the first check that failed. */
static int near(double a, double b) { double d = a - b; return d < 1e-12 && d > -1e-12; }
int main(void) {
  if (!near(__builtin_log2(2.75), 1.4594316186372973)) return 1;
  if (__builtin_log2(8.0) != 3.0 || __builtin_exp2(-1.0) != 0.5) return 2;
  if (!near(__builtin_exp2(3.5), 11.313708498984761)) return 3;
  if (__builtin_log2f(8.0f) != 3.0f || __builtin_exp2f(3.0f) != 8.0f) return 4;
  if (__builtin_log2l(16.0L) != 4.0L || __builtin_exp2l(2.0L) != 4.0L) return 5;
  if (__builtin_trunc(-2.5) != -2.0 || __builtin_round(2.5) != 3.0 || __builtin_rint(2.5) != 2.0) return 6;
  if (__builtin_nearbyint(3.5) != 4.0 || __builtin_cbrt(27.0) != 3.0 || __builtin_truncf(1.75f) != 1.0f) return 7;
  if (__builtin_fmin(1.5, -2.0) != -2.0 || __builtin_fmax(1.5, -2.0) != 1.5 || __builtin_fmaxf(1.0f, 2.0f) != 2.0f) return 8;
  if (__builtin_hypot(3.0, 4.0) != 5.0 || __builtin_fdim(5.0, 2.0) != 3.0 || __builtin_remainder(7.0, 2.0) != -1.0) return 9;
  if (__builtin_fma(2.0, 3.0, 1.0) != 7.0 || __builtin_fmaf(2.0f, 3.0f, 0.5f) != 6.5f) return 10;
  if (__builtin_ilogb(40.0) != 5 || __builtin_logb(40.0) != 5.0) return 11;
  if (__builtin_lround(2.5) != 3 || __builtin_llrint(-3.5) != -4 || __builtin_lrintf(2.5f) != 2 || __builtin_llround(-2.5) != -3) return 12;
  if (__builtin_scalbn(1.5, 3) != 12.0 || __builtin_scalbnf(1.0f, -1) != 0.5f) return 13;
  if (__builtin_expm1(0.0) != 0.0 || __builtin_log1p(0.0) != 0.0) return 14;
  return 0;
}
