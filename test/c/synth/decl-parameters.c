// synth-c23/decl-parameters.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// parameter lists: (void), C23's (), unnamed parameters in definitions, arrays with static, [*], variadic
// ISO/IEC 9899:2024 6.7.7.4, 6.9.2; Annex A: parameter-type-list, parameter-list, parameter-declaration
// M.2: unnamed parameters in function definitions; () declares no parameters
#include <stdio.h>

static int nothing();
static int nothing(void) { return 3; }
static int second(int, int b) { return b; }
static int first3(int a[static 3]) { return a[0] + a[1] + a[2]; }
static int qual(int a[const 2]) { return a[1]; }
static int vsum(int n, int a[*]);
static int vsum(int n, int a[n]) { int s = 0; for (int i = 0; i < n; i++) s += a[i]; return s; }
static int count(int n, ...) { return n; }
static int fnparam(int f(int), int x) { return f(x); }
static int neg(int x) { return -x; }
static int star(int, int[*]);
static int star(int n, int a[n]) { return a[n - 1]; }
static int cstat(int a[const static 2]) { return a[1] + 1; }
static int restr(int *restrict[2]);
static int restr(int *restrict p[2]) { return *p[0]; }

int main(void) {
  int a[4] = {1, 2, 3, 4};
  printf("%s = %d\n", "nothing()", nothing());
  printf("%s = %d\n", "second(1, 2)", second(1, 2));
  printf("%s = %d\n", "first3(a)", first3(a));
  printf("%s = %d\n", "qual(a)", qual(a));
  printf("%s = %d\n", "vsum(4, a)", vsum(4, a));
  printf("%s = %d\n", "count(2, 1.0, \"x\")", count(2, 1.0, "x"));
  printf("%s = %d\n", "fnparam(neg, 5)", fnparam(neg, 5));
  printf("%s = %d\n", "star(3, a)", star(3, a));
  printf("%s = %d\n", "cstat(a)", cstat(a));
  printf("%s = %d\n", "restr((int *[2]){a + 3, a})", restr((int *[2]){a + 3, a}));
  return 0;
}
