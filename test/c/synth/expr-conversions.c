// synth-c23/expr-conversions.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// integer promotions, signed and unsigned mixing, floating to integer
// ISO/IEC 9899:2024 6.3.1; Annex A: (conversions within expressions)
#include <stdio.h>

int main(void) {
  unsigned char uc = 250;
  signed char sc = -6;
  unsigned short us = 65535;
  long long ll = -1;
  unsigned u = 3;
  printf("%s = %d\n", "uc + 10", uc + 10);
  printf("%s = %d\n", "sc * 2", sc * 2);
  printf("%s = %d\n", "us + 1", us + 1);
  printf("%s = %d\n", "_Generic(uc + uc, int: 1, default: 0)", _Generic(uc + uc, int: 1, default: 0));
  printf("%s = %d\n", "ll < u", ll < u);
  printf("%s = %d\n", "(int)2.99", (int)2.99);
  printf("%s = %d\n", "(int)-2.99", (int)-2.99);
  printf("%s = %u\n", "(unsigned)-1", (unsigned)-1);
  printf("%s = %llu\n", "(unsigned long long)ll", (unsigned long long)ll);
  printf("%s = %g\n", "(double)(1ull << 53)", (double)(1ull << 53));
  printf("%s = %d\n", "(int)(char)65", (int)(char)65);
  printf("%s = %d\n", "_Generic(1 + 1u, unsigned: 1, default: 0)", _Generic(1 + 1u, unsigned: 1, default: 0));
  printf("%s = %d\n", "_Generic(1 + 1ll, long long: 1, default: 0)", _Generic(1 + 1ll, long long: 1, default: 0));
  return 0;
}
