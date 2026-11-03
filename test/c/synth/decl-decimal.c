// synth-c23/decl-decimal.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// the decimal floating types _Decimal32, _Decimal64, _Decimal128 and their suffixes
// ISO/IEC 9899:2024 6.2.5, 6.4.4.2; Annex A: type-specifier (_Decimal32, _Decimal64, _Decimal128), floating-suffix (df, dd, dl)
// M.2: decimal floating types
#include <stdio.h>

int main(void) {
  _Decimal32 a = 1.1df;
  _Decimal64 b = 2.2dd;
  _Decimal128 c = 3.3dl;
  _Decimal64 s = a + b;
  printf("%s = %d\n", "(int)(s * 10)", (int)(s * 10));
  printf("%s = %d\n", "(int)(c * 10)", (int)(c * 10));
  printf("%s = %d\n", "s == 3.3dd", s == 3.3dd);
  printf("%s = %d\n", "(int)(b / 2.0dd * 10)", (int)(b / 2.0dd * 10));
  printf("%s = %d\n", "_Generic(1.0dd, _Decimal64: 1, default: 0)", _Generic(1.0dd, _Decimal64: 1, default: 0));
  return 0;
}
