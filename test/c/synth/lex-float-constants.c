// synth-c23/lex-float-constants.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// floating constants: decimal, hexadecimal, exponents, suffixes
// ISO/IEC 9899:2024 6.4.4.2; Annex A: floating-constant, decimal-floating-constant, hexadecimal-floating-constant, exponent-part, binary-exponent-part, floating-suffix
#include <stdio.h>

int main(void) {
  printf("%s = %g\n", "1.5", 1.5);
  printf("%s = %g\n", ".5", .5);
  printf("%s = %g\n", "5.", 5.);
  printf("%s = %g\n", "1e3", 1e3);
  printf("%s = %g\n", "2.5E-2", 2.5E-2);
  printf("%s = %g\n", "1.5e+2", 1.5e+2);
  printf("%s = %g\n", "(double)1.25f", (double)1.25f);
  printf("%s = %g\n", "(double)0.75F", (double)0.75F);
  printf("%s = %g\n", "(double)1.25L", (double)1.25L);
  printf("%s = %g\n", "0x1p4", 0x1p4);
  printf("%s = %g\n", "0x1.8p1", 0x1.8p1);
  printf("%s = %g\n", "0X.8P-1", 0X.8P-1);
  printf("%s = %g\n", "0xAp0", 0xAp0);
  printf("%s = %d\n", "_Generic(1.0f, float: 1, default: 0)", _Generic(1.0f, float: 1, default: 0));
  printf("%s = %d\n", "_Generic(1.0, double: 1, default: 0)", _Generic(1.0, double: 1, default: 0));
  printf("%s = %d\n", "_Generic(1.0l, long double: 1, default: 0)", _Generic(1.0l, long double: 1, default: 0));
  return 0;
}
