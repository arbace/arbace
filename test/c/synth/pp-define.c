// synth-c23/pp-define.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// macros: object-like, function-like, nesting, # and ##, empty arguments, no recursion, #undef
// ISO/IEC 9899:2024 6.10.5; Annex A: control-line (#define, #undef), replacement-list, identifier-list
#include <stdio.h>

#define TEN 10
#define SQ(x) ((x) * (x))
#define ADD(a, b) ((a) + (b))
#define STR(x) #x
#define XSTR(x) STR(x)
#define CAT(a, b) a##b
#define EMPTY
#define SELF SELF
#define ID(x) x
#define NONE() 7
#define TWICE(f, x) f(f(x))
static int SELF = 3;
#define GONE 1
#undef GONE
#ifndef GONE
static int gone = 1;
#endif

int main(void) {
  printf("%s = %d\n", "TEN", TEN);
  printf("%s = %d\n", "SQ(TEN)", SQ(TEN));
  printf("%s = %d\n", "ADD(1, SQ(2))", ADD(1, SQ(2)));
  printf("%s = %s\n", "STR(a + b)", STR(a + b));
  printf("%s = %s\n", "XSTR(TEN)", XSTR(TEN));
  printf("%s = %d\n", "CAT(TE, N)", CAT(TE, N));
  printf("%s = %d\n", "ID() 5 EMPTY", ID() 5 EMPTY);
  printf("%s = %d\n", "SELF", SELF);
  printf("%s = %d\n", "NONE()", NONE());
  printf("%s = %d\n", "TWICE(SQ, 3)", TWICE(SQ, 3));
  printf("%s = %d\n", "gone", gone);
  printf("%s = %s\n", "STR(\"q\\n\")", STR("q\n"));
  printf("%s = %d\n", "CAT(1, 2)", CAT(1, 2));
  return 0;
}
