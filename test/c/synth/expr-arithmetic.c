// synth-c23/expr-arithmetic.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// multiplicative and additive operators, pointer arithmetic, the usual conversions
// ISO/IEC 9899:2024 6.5.6, 6.5.7, 6.3.1.8; Annex A: multiplicative-expression, additive-expression
#include <stdio.h>

int main(void) {
  int a[5] = {1, 2, 3, 4, 5};
  int *p = a + 1;
  int *q = &a[4];
  printf("%s = %d\n", "7 * 6", 7 * 6);
  printf("%s = %d\n", "17 / 5", 17 / 5);
  printf("%s = %d\n", "17 % 5", 17 % 5);
  printf("%s = %d\n", "-17 / 5", -17 / 5);
  printf("%s = %d\n", "-17 % 5", -17 % 5);
  printf("%s = %d\n", "17 / -5", 17 / -5);
  printf("%s = %d\n", "17 % -5", 17 % -5);
  printf("%s = %d\n", "3 + 4", 3 + 4);
  printf("%s = %d\n", "3 - 10", 3 - 10);
  printf("%s = %d\n", "*(p + 2)", *(p + 2));
  printf("%s = %d\n", "*(q - 1)", *(q - 1));
  printf("%s = %td\n", "q - p", q - p);
  printf("%s = %td\n", "p - q", p - q);
  printf("%s = %d\n", "-1 < 0u", -1 < 0u);
  printf("%s = %d\n", "(unsigned short)65535 + 1", (unsigned short)65535 + 1);
  printf("%s = %g\n", "1 / 2.0", 1 / 2.0);
  printf("%s = %u\n", "0u - 1u", 0u - 1u);
  printf("%s = %lld\n", "1000000LL * 1000000", 1000000LL * 1000000);
  printf("%s = %d\n", "'a' + 1", 'a' + 1);
  return 0;
}
