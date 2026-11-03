// synth-c23/expr-primary.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// primary expressions: identifiers, constants, strings, parentheses, __func__
// ISO/IEC 9899:2024 6.5.2, 6.4.2.2; Annex A: primary-expression
#include <stdio.h>

static int v = 9;
static const char *name(void) { return __func__; }

int main(void) {
  printf("%s = %d\n", "v", v);
  printf("%s = %d\n", "(v)", (v));
  printf("%s = %d\n", "((v + 1))", ((v + 1)));
  printf("%s = %d\n", "\"xyz\"[1]", "xyz"[1]);
  printf("%s = %s\n", "name()", name());
  printf("%s = %s\n", "__func__", __func__);
  return 0;
}
