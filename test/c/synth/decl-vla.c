// synth-c23/decl-vla.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// variable length arrays: block scope, parameters, two dimensions, typedef
// ISO/IEC 9899:2024 6.7.7.3; Annex A: array-declarator
// M.2: variably modified types mandatory
#include <stdio.h>

static int trace(int n, int m[n][n]) { int t = 0; for (int i = 0; i < n; i++) t += m[i][i]; return t; }

int main(void) {
  int n = 4;
  int m[n][n];
  for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) m[i][j] = i * n + j;
  typedef int row[n];
  n = 9;
  row r;
  int (*pm)[4] = m;
  printf("%s = %d\n", "trace(4, m)", trace(4, m));
  printf("%s = %zu\n", "sizeof r / sizeof r[0]", sizeof r / sizeof r[0]);
  printf("%s = %d\n", "pm[3][3]", pm[3][3]);
  return 0;
}
