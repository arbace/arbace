// synth-c23/decl-floatn-wide.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// Annex H's _Float16, _Float128 and _Float64x and their suffixes, printed as double (exact values)
// ISO/IEC 9899:2024 H.2, H.4; Annex A: type-specifier (_Float16, _Float128, _Float64x), floating-suffix (f16, f128, f64x)
#include <stdio.h>

int main(void) {
  _Float16 h = 0.5f16;
  _Float32 hs = h * 4;
  _Float16 sum = h + h;
  _Float128 q = 2.0f128;
  _Float64x x = 1.25f64x;
  printf("%s = %g\n", "(double)h", (double)h);
  printf("%s = %g\n", "(double)hs", (double)hs);
  printf("%s = %g\n", "(double)sum", (double)sum);
  printf("%s = %d\n", "_Generic(h * h, _Float16: 1, default: 0)", _Generic(h * h, _Float16: 1, default: 0));
  printf("%s = %g\n", "(double)q", (double)q);
  printf("%s = %g\n", "(double)x", (double)x);
  printf("%s = %g\n", "(double)(q * x)", (double)(q * x));
  printf("%s = %d\n", "_Generic(q, _Float128: 1, default: 0)", _Generic(q, _Float128: 1, default: 0));
  return 0;
}
