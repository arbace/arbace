// synth-c23/ext-functions.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// function definitions: prototypes, recursion, mutual recursion, static and extern functions
// ISO/IEC 9899:2024 6.9.2; Annex A: translation-unit, external-declaration, function-definition, function-body
#include <stdio.h>

static int is_odd(int n);
static int is_even(int n) { return n == 0 ? 1 : is_odd(n - 1); }
static int is_odd(int n) { return n == 0 ? 0 : is_even(n - 1); }
static long long fact(int n) { return n <= 1 ? 1 : n * fact(n - 1); }
extern int ext_fn(int);
int ext_fn(int x) { return x + 100; }
static void out(int *p) { *p = 9; }

int main(void) {
  int v = 0;
  out(&v);
  printf("%s = %d\n", "is_even(10)", is_even(10));
  printf("%s = %d\n", "is_odd(7)", is_odd(7));
  printf("%s = %lld\n", "fact(20)", fact(20));
  printf("%s = %d\n", "ext_fn(1)", ext_fn(1));
  printf("%s = %d\n", "v", v);
  return 0;
}
