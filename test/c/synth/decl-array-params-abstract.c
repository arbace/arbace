// synth-c23/decl-array-params-abstract.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// array parameters with static in prototypes without names (abstract declarators)
// ISO/IEC 9899:2024 6.7.7.4, 6.7.8; Annex A: array-abstract-declarator ([ static type-qualifier-list_opt assignment-expression ], [ type-qualifier-list static assignment-expression ])
#include <stdio.h>

static int stat2(int[static 2]);
static int stat2(int a[static 2]) { return a[0] * a[1]; }
static int cstat2(int[const static 2]);
static int cstat2(int a[const static 2]) { return a[1]; }

int main(void) {
  int a[3] = {3, 4, 5};
  printf("%s = %d\n", "stat2(a)", stat2(a));
  printf("%s = %d\n", "cstat2(a + 1)", cstat2(a + 1));
  return 0;
}
