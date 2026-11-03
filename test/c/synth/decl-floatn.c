// synth-c23/decl-floatn.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// the interchange and extended floating types of Annex H: _Float32, _Float64, _Float32x and their suffixes
// ISO/IEC 9899:2024 H.2, H.4; Annex A: type-specifier (_FloatN, _FloatNx), floating-suffix (f32, f64, f32x)
#include <stdio.h>

int main(void) {
  _Float32 a = 1.5f32;
  _Float64 b = 2.25f64;
  _Float32x c = 3.0f32x;
  _Float64 d = a * b;
  printf("%s = %g\n", "(double)a", (double)a);
  printf("%s = %g\n", "(double)b", (double)b);
  printf("%s = %g\n", "(double)c", (double)c);
  printf("%s = %g\n", "(double)d", (double)d);
  printf("%s = %d\n", "_Generic(1.0f32, _Float32: 1, default: 0)", _Generic(1.0f32, _Float32: 1, default: 0));
  printf("%s = %d\n", "_Generic(a + b, _Float64: 1, default: 0)", _Generic(a + b, _Float64: 1, default: 0));
  return 0;
}
