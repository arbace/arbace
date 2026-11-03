// synth-c23/pp-variadic.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// variadic macros: __VA_ARGS__, C23's __VA_OPT__, stringized arguments
// ISO/IEC 9899:2024 6.10.5.1; Annex A: control-line (#define ... ( identifier-list , ... ) or ( ... )), va-opt-replacement
// M.2: __VA_OPT__
#include <stdio.h>

#define COUNT(...) count_(0 __VA_OPT__(,) __VA_ARGS__)
static int count_(int z, ...) { (void)z; return 0; }
#define SUM(first, ...) (first __VA_OPT__(+ sum_rest(__VA_ARGS__)))
#define sum_rest(...) (__VA_ARGS__)
#define SHOW(...) #__VA_ARGS__
#define F(a, ...) f_(a __VA_OPT__(, __VA_ARGS__))
static int f_(int a, ...) { return a; }
#define NARGS(...) NARGS_(__VA_ARGS__ __VA_OPT__(,) 3, 2, 1, 0)
#define NARGS_(a, b, c, n, ...) n
#define EMPTYOPT(...) __VA_OPT__()x
static int x = 5;

int main(void) {
  printf("%s = %d\n", "COUNT()", COUNT());
  printf("%s = %d\n", "COUNT(1, 2)", COUNT(1, 2));
  printf("%s = %d\n", "SUM(5)", SUM(5));
  printf("%s = %d\n", "SUM(5, 6)", SUM(5, 6));
  printf("%s = %s\n", "SHOW(a, b,  c)", SHOW(a, b,  c));
  printf("%s = %s\n", "SHOW()", SHOW());
  printf("%s = %d\n", "F(4)", F(4));
  printf("%s = %d\n", "F(4, 5, 6)", F(4, 5, 6));
  printf("%s = %d\n", "NARGS()", NARGS());
  printf("%s = %d\n", "NARGS(a)", NARGS(a));
  printf("%s = %d\n", "NARGS(a, b)", NARGS(a, b));
  printf("%s = %d\n", "NARGS(a, b, c)", NARGS(a, b, c));
  printf("%s = %d\n", "EMPTYOPT(1)", EMPTYOPT(1));
  return 0;
}
