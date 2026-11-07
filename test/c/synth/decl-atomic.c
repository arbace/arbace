// synth-c23/decl-atomic.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// atomic types without stdatomic.h: _Atomic qualifier and specifier, compound assignment, increments
// ISO/IEC 9899:2024 6.7.3.5, 6.5.17.3; Annex A: atomic-type-specifier, type-qualifier (_Atomic)
#include <stdio.h>

int main(void) {
  _Atomic int a = 1;
  a += 4;
  a++;
  ++a;
  a *= 2;
  _Atomic(unsigned long long) b = 10;
  b -= 3;
  int plain = a;
  printf("%s = %d\n", "plain", plain);
  printf("%s = %llu\n", "(unsigned long long)b", (unsigned long long)b);
  printf("%s = %d\n", "_Generic(a + 0, int: 1, default: 0)", _Generic(a + 0, int: 1, default: 0));
  return 0;
}
