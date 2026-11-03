// synth-c23/decl-enum.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// enumerations: implicit and explicit values, negative values, a trailing comma, use as integers
// ISO/IEC 9899:2024 6.7.3.3; Annex A: enum-specifier, enumerator-list, enumerator
#include <stdio.h>

enum fruit { APPLE, PEAR = 5, PLUM, FIG = -2, KIWI, };
enum { ANON = 100 };
typedef enum { T0, T1 } tenum;

int main(void) {
  enum fruit f = PLUM;
  tenum t = T1;
  printf("%s = %d\n", "APPLE", APPLE);
  printf("%s = %d\n", "PEAR", PEAR);
  printf("%s = %d\n", "PLUM", PLUM);
  printf("%s = %d\n", "FIG", FIG);
  printf("%s = %d\n", "KIWI", KIWI);
  printf("%s = %d\n", "ANON", ANON);
  printf("%s = %d\n", "f", f);
  printf("%s = %d\n", "t", t);
  printf("%s = %d\n", "f == 6", f == 6);
  printf("%s = %d\n", "_Generic(PEAR, int: 1, default: 0)", _Generic(PEAR, int: 1, default: 0));
  return 0;
}
