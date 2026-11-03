// synth-c23/expr-conditional-comma.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// conditional and comma operators
// ISO/IEC 9899:2024 6.5.16, 6.5.18; Annex A: conditional-expression, expression
#include <stdio.h>

int main(void) {
  int a = 1, b = 2, c;
  c = (a++, b++, a + b);
  int *p = a > 5 ? &a : &b;
  const char *s = c ? "yes" : "no";
  printf("%s = %d\n", "c", c);
  printf("%s = %d\n", "a", a);
  printf("%s = %d\n", "b", b);
  printf("%s = %d\n", "*p", *p);
  printf("%s = %d\n", "a ? b : c", a ? b : c);
  printf("%s = %d\n", "0 ? 1 : 2 ? 3 : 4", 0 ? 1 : 2 ? 3 : 4);
  printf("%s = %s\n", "s", s);
  printf("%s = %g\n", "1 ? 1 : 2.5", 1 ? 1 : 2.5);
  return 0;
}
