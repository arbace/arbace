// synth-c23/decl-tag-compat.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// C23's tag compatibility: a structure, union or enumeration redefined with the same content
// ISO/IEC 9899:2024 6.7.3.4, 6.2.7; Annex A: struct-or-union-specifier
// M.2: a tag redefined with the same content is the same type
#include <stdio.h>

struct pair { int a, b; };
struct pair { int a, b; };
union num { int i; double d; };
union num { int i; double d; };
enum color { RED, GREEN };
enum color { RED, GREEN };
static int sum(struct pair { int a, b; } p) { return p.a + p.b; }

int main(void) {
  struct pair p = {3, 4};
  union num n = {.i = 5};
  printf("%s = %d\n", "sum(p)", sum(p));
  printf("%s = %d\n", "n.i", n.i);
  printf("%s = %d\n", "GREEN", GREEN);
  return 0;
}
