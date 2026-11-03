// synth-c23/expr-assignment.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// simple and compound assignment
// ISO/IEC 9899:2024 6.5.17; Annex A: assignment-expression, assignment-operator
#include <stdio.h>

int main(void) {
  int x = 100, y;
  int r[11];
  r[0] = (x *= 3);
  r[1] = (x /= 7);
  r[2] = (x %= 11);
  r[3] = (x += 40);
  r[4] = (x -= 2);
  r[5] = (x <<= 3);
  r[6] = (x >>= 1);
  r[7] = (x &= 0x7C);
  r[8] = (x ^= 0x55);
  r[9] = (x |= 0x100);
  r[10] = (y = x = 5);
  printf("%s = %d\n", "r[0]", r[0]);
  printf("%s = %d\n", "r[1]", r[1]);
  printf("%s = %d\n", "r[2]", r[2]);
  printf("%s = %d\n", "r[3]", r[3]);
  printf("%s = %d\n", "r[4]", r[4]);
  printf("%s = %d\n", "r[5]", r[5]);
  printf("%s = %d\n", "r[6]", r[6]);
  printf("%s = %d\n", "r[7]", r[7]);
  printf("%s = %d\n", "r[8]", r[8]);
  printf("%s = %d\n", "r[9]", r[9]);
  printf("%s = %d\n", "r[10]", r[10]);
  printf("%s = %d\n", "y", y);
  return 0;
}
