// synth-c23/lex-keywords-c23.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// C23's new keywords, each used once
// ISO/IEC 9899:2024 6.4.1; Annex A: keyword
// M.2: alignas, alignof, bool, constexpr, false, nullptr, static_assert, thread_local, true, typeof, typeof_unqual keywords
#include <stdio.h>

static thread_local int tl = 3;
static alignas(8) int al = 4;
static constexpr int ce = 5;
static_assert(ce == 5);

int main(void) {
  bool b = true;
  typeof(ce) t = ce;
  typeof_unqual(ce) u = 6;
  int *p = nullptr;
  u = u + (b != false);
  printf("%s = %d\n", "b", b);
  printf("%s = %d\n", "false", false);
  printf("%s = %d\n", "tl", tl);
  printf("%s = %d\n", "al", al);
  printf("%s = %d\n", "t", t);
  printf("%s = %d\n", "u", u);
  printf("%s = %d\n", "p == nullptr", p == nullptr);
  printf("%s = %zu\n", "alignof(char)", alignof(char));
  printf("%s = %d\n", "_Generic(true, bool: 1, default: 0)", _Generic(true, bool: 1, default: 0));
  return 0;
}
