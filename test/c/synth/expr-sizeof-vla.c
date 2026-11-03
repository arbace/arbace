// synth-c23/expr-sizeof-vla.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// sizeof of variable length arrays, evaluated at run time
// ISO/IEC 9899:2024 6.5.4.4, 6.7.7.3; Annex A: unary-expression (sizeof)
#include <stdio.h>

static int width(int n) { int a[n]; return (int)(sizeof a / sizeof a[0]); }

int main(void) {
  int n = 6;
  int m[n][n + 1];
  int k = 0;
  size_t s = sizeof(int[++k]);
  printf("%s = %d\n", "width(5)", width(5));
  printf("%s = %zu\n", "sizeof m / sizeof m[0]", sizeof m / sizeof m[0]);
  printf("%s = %zu\n", "sizeof m[0] / sizeof m[0][0]", sizeof m[0] / sizeof m[0][0]);
  printf("%s = %d\n", "k", k);
  printf("%s = %zu\n", "s / sizeof(int)", s / sizeof(int));
  return 0;
}
