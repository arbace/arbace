// synth-c23/stmt-for-declaration.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// the for statement's declaration clause: objects, several declarators, a static assertion
// ISO/IEC 9899:2024 6.8.6.1, 6.7.12; Annex A: iteration-statement (for ( declaration expression_opt ; expression_opt ) statement), static_assert-declaration
#include <stdio.h>

int main(void) {
  int n = 0;
  for (int i = 0, j = 3; i < j; i++) n += i * j;
  int m = 0;
  for (static_assert(1, "in a for"); m < 2; m++) {}
  int k = 0;
  for (static_assert(sizeof(int) >= 2); k < 5; k += 2) {}
  printf("%s = %d\n", "n", n);
  printf("%s = %d\n", "m", m);
  printf("%s = %d\n", "k", k);
  return 0;
}
