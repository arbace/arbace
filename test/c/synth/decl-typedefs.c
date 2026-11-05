// synth-c23/decl-typedefs.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// typedef names: scopes, redeclaration, function and array types, shadowing
// ISO/IEC 9899:2024 6.7.9; Annex A: typedef-name
#include <stdio.h>

typedef int T;
typedef int T;
typedef int fn(int);
typedef T arr3[3];
static fn twice;
static int twice(int x) { return 2 * x; }
typedef struct { int v; } box;

int main(void) {
  arr3 a = {1, 2, 3};
  box b = {4};
  int r;
  { typedef double T; T d = 1.5; r = (int)(d * 2); }
  T T2 = 5;
  { int T = 6; r += T; }
  printf("%s = %d\n", "a[2]", a[2]);
  printf("%s = %d\n", "b.v", b.v);
  printf("%s = %d\n", "twice(T2)", twice(T2));
  printf("%s = %d\n", "r", r);
  return 0;
}
