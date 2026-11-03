// synth-c23/expr-floating.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// floating arithmetic and comparisons (IEC 60559 double and float)
// ISO/IEC 9899:2024 6.5, Annex F; Annex A: (arithmetic on floating types)
#include <stdio.h>

int main(void) {
  double a = 0.1, b = 0.2;
  float f = 1.0f / 3.0f;
  double z = 0.0;
  printf("%s = %.17g\n", "a + b", a + b);
  printf("%s = %.9g\n", "(double)f", (double)f);
  printf("%s = %d\n", "a + b == 0.3", a + b == 0.3);
  printf("%s = %g\n", "1.0 / 4", 1.0 / 4);
  printf("%s = %g\n", "-z", -z);
  printf("%s = %d\n", "1.0 / 3 > 0.333", 1.0 / 3 > 0.333);
  printf("%s = %g\n", "1e308 * 10 > 1e308 ? 1.0 : 0.0", 1e308 * 10 > 1e308 ? 1.0 : 0.0);
  printf("%s = %.3f\n", "2.0 * 1.5 - 0.25", 2.0 * 1.5 - 0.25);
  printf("%s = %d\n", "(int)(7.0 / 2.0)", (int)(7.0 / 2.0));
  return 0;
}
