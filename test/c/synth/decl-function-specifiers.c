// synth-c23/decl-function-specifiers.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// function specifiers inline and _Noreturn, and the noreturn attribute
// ISO/IEC 9899:2024 6.7.5, 6.7.13.7; Annex A: function-specifier
#include <stdio.h>
#include <stdlib.h>

static inline int sq(int x) { return x * x; }
inline int cube(int x) { return x * x * x; }
extern inline int cube(int x);
_Noreturn static void stop(int s) { exit(s); }
[[noreturn]] static void halt(void) { exit(0); }

int main(void) {
  if (sq(2) == 5) stop(1);
  if (cube(2) == 9) halt();
  printf("%s = %d\n", "sq(7)", sq(7));
  printf("%s = %d\n", "cube(3)", cube(3));
  return 0;
}
