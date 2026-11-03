// synth-c23/expr-cast.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// cast expressions: conversions between integer, floating, pointer and void
// ISO/IEC 9899:2024 6.5.5, 6.3; Annex A: cast-expression
#include <stdio.h>

int main(void) {
  double d = 3.75;
  int n = -7;
  void *vp = &n;
  (void)d;
  printf("%s = %d\n", "(int)d", (int)d);
  printf("%s = %d\n", "(int)-d", (int)-d);
  printf("%s = %d\n", "(unsigned char)300", (unsigned char)300);
  printf("%s = %d\n", "(signed char)200", (signed char)200);
  printf("%s = %g\n", "(double)7 / 2", (double)7 / 2);
  printf("%s = %u\n", "(unsigned)n", (unsigned)n);
  printf("%s = %d\n", "*(int *)vp", *(int *)vp);
  printf("%s = %d\n", "(int)(long long)1e10 == 1410065408", (int)(long long)1e10 == 1410065408);
  printf("%s = %g\n", "(double)(float)0.1 == 0.1 ? 1.0 : 0.0", (double)(float)0.1 == 0.1 ? 1.0 : 0.0);
  printf("%s = %d\n", "(bool)0.5", (bool)0.5);
  printf("%s = %d\n", "(bool)2", (bool)2);
  printf("%s = %d\n", "(int)(unsigned short)-1", (int)(unsigned short)-1);
  return 0;
}
