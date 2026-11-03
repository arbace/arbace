// synth-c23/decl-scopes.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// scopes and linkage: shadowing, tentative definitions, block-scope extern and static
// ISO/IEC 9899:2024 6.2.1, 6.2.2, 6.9.3; Annex A: declaration, init-declarator-list, init-declarator
#include <stdio.h>

int tent;
int tent = 3;
int tent;
static int hidden = 1;
static int read_hidden(void) { extern int tent; return tent + hidden; }

int main(void) {
  int hidden = 10;
  int inner;
  { int hidden = 20; inner = hidden; { extern int tent; inner += tent; } }
  int a = 1, b = a + 1, *pb = &b;
  printf("%s = %d\n", "hidden", hidden);
  printf("%s = %d\n", "inner", inner);
  printf("%s = %d\n", "read_hidden()", read_hidden());
  printf("%s = %d\n", "*pb", *pb);
  return 0;
}
