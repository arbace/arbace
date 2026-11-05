// synth-c23/expr-shift-relational.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// shift, relational and equality operators
// ISO/IEC 9899:2024 6.5.8, 6.5.9, 6.5.10; Annex A: shift-expression, relational-expression, equality-expression
#include <stdio.h>

int main(void) {
  int a[3];
  unsigned u = 0x80000000u;
  printf("%s = %d\n", "1 << 10", 1 << 10);
  printf("%s = %d\n", "1024 >> 3", 1024 >> 3);
  printf("%s = %u\n", "u >> 31", u >> 31);
  printf("%s = %u\n", "u << 1", u << 1);
  printf("%s = %llu\n", "1ull << 63 >> 62", 1ull << 63 >> 62);
  printf("%s = %d\n", "3 < 4", 3 < 4);
  printf("%s = %d\n", "4 < 3", 4 < 3);
  printf("%s = %d\n", "3 > 2", 3 > 2);
  printf("%s = %d\n", "3 <= 3", 3 <= 3);
  printf("%s = %d\n", "3 >= 4", 3 >= 4);
  printf("%s = %d\n", "&a[0] < &a[2]", &a[0] < &a[2]);
  printf("%s = %d\n", "&a[2] >= &a[1]", &a[2] >= &a[1]);
  printf("%s = %d\n", "1 == 1", 1 == 1);
  printf("%s = %d\n", "1 != 1", 1 != 1);
  printf("%s = %d\n", "(1 < 2) == (2 < 3)", (1 < 2) == (2 < 3));
  printf("%s = %d\n", "-1 < 1u", -1 < 1u);
  return 0;
}
