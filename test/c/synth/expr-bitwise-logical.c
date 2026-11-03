// synth-c23/expr-bitwise-logical.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// bitwise and logical operators, short-circuit evaluation
// ISO/IEC 9899:2024 6.5.11-6.5.15; Annex A: AND-expression, exclusive-OR-expression, inclusive-OR-expression, logical-AND-expression, logical-OR-expression
#include <stdio.h>

static int calls;
static int hit(int r) { calls++; return r; }

int main(void) {
  int and1 = hit(0) && hit(1);
  int or1 = hit(1) || hit(0);
  int and2 = hit(1) && hit(2);
  printf("%s = %d\n", "0xF0 & 0x3C", 0xF0 & 0x3C);
  printf("%s = %d\n", "0xF0 ^ 0x3C", 0xF0 ^ 0x3C);
  printf("%s = %d\n", "0xF0 | 0x0F", 0xF0 | 0x0F);
  printf("%s = %d\n", "and1", and1);
  printf("%s = %d\n", "or1", or1);
  printf("%s = %d\n", "and2", and2);
  printf("%s = %d\n", "calls", calls);
  printf("%s = %d\n", "0 || 0", 0 || 0);
  printf("%s = %d\n", "5 && 7", 5 && 7);
  printf("%s = %d\n", "!(1 && 0)", !(1 && 0));
  return 0;
}
