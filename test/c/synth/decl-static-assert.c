// synth-c23/decl-static-assert.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// static assertions with and without a message at file, block and member scope
// ISO/IEC 9899:2024 6.7.12; Annex A: static_assert-declaration
// M.2: static_assert without a message; static_assert a keyword
#include <stdio.h>

static_assert(sizeof(int) >= 2, "int is at least 16 bits");
static_assert(1 + 1 == 2);
_Static_assert(1, "old spelling");
_Static_assert(2 > 1);
struct sa { int x; static_assert(1); };

int main(void) {
  static_assert(sizeof(struct sa) == sizeof(int));
  int ok = 1;
  { static_assert(2 > 1, "block"); ok = 2; }
  printf("%s = %d\n", "ok", ok);
  printf("%s = %d\n", "(int)sizeof(struct sa) == (int)sizeof(int)", (int)sizeof(struct sa) == (int)sizeof(int));
  return 0;
}
