// synth-c23/decl-complex.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// complex types: _Complex arithmetic with complex.h's I, creal and cimag
// ISO/IEC 9899:2024 6.2.5, 7.3; Annex A: type-specifier (_Complex)
#include <stdio.h>
#include <complex.h>

int main(void) {
  double _Complex z = 1.0 + 2.0 * I;
  double _Complex w = z * z;
  float _Complex fz = 3.0f;
  double _Complex c = conj(z);
  printf("%s = %g\n", "creal(w)", creal(w));
  printf("%s = %g\n", "cimag(w)", cimag(w));
  printf("%s = %g\n", "crealf(fz)", crealf(fz));
  printf("%s = %g\n", "cimag(c)", cimag(c));
  printf("%s = %d\n", "z == 1.0 + 2.0 * I", z == 1.0 + 2.0 * I);
  printf("%s = %g\n", "creal(z + w)", creal(z + w));
  return 0;
}
