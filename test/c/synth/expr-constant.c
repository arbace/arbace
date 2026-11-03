// synth-c23/expr-constant.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// constant expressions: array sizes, case labels, enumerators, static initializers
// ISO/IEC 9899:2024 6.6; Annex A: constant-expression
#include <stdio.h>

enum { K = 3 * 4 + 1 };
static int arr[K - 10];
static int *const ptr = &arr[2];
static const int sz = (int)(sizeof arr / sizeof arr[0]);
static int pick(int v) { switch (v) { case K: return 1; case K + 1: return 2; default: return 0; } }

int main(void) {
  printf("%s = %d\n", "K", K);
  printf("%s = %d\n", "sz", sz);
  printf("%s = %d\n", "(int)(ptr - arr)", (int)(ptr - arr));
  printf("%s = %d\n", "pick(13)", pick(13));
  printf("%s = %d\n", "pick(14)", pick(14));
  printf("%s = %d\n", "pick(0)", pick(0));
  printf("%s = %d\n", "(1 ? 2 : 3) * (4 , 5)", (1 ? 2 : 3) * (4 , 5));
  return 0;
}
