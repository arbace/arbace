// synth-c23/expr-generic.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// generic selections: type names, default, lvalue conversion, pointers and qualifiers
// ISO/IEC 9899:2024 6.5.2.1; Annex A: generic-selection, generic-assoc-list, generic-association
#include <stdio.h>

#define KIND(x) _Generic((x), int: 1, unsigned: 2, long: 3, double: 4, char *: 5, const char *: 6, int *: 7, default: 0)

int main(void) {
  const int ci = 1;
  int a[2] = {0};
  char s[] = "s";
  short sh = 1;
  printf("%s = %d\n", "KIND(1)", KIND(1));
  printf("%s = %d\n", "KIND(1u)", KIND(1u));
  printf("%s = %d\n", "KIND(1l)", KIND(1l));
  printf("%s = %d\n", "KIND(1.0)", KIND(1.0));
  printf("%s = %d\n", "KIND(s)", KIND(s));
  printf("%s = %d\n", "KIND((const char *)s)", KIND((const char *)s));
  printf("%s = %d\n", "KIND(a)", KIND(a));
  printf("%s = %d\n", "KIND(ci)", KIND(ci));
  printf("%s = %d\n", "KIND(1.0f)", KIND(1.0f));
  printf("%s = %d\n", "KIND(sh)", KIND(sh));
  printf("%s = %d\n", "KIND(+sh)", KIND(+sh));
  printf("%s = %d\n", "_Generic(ci, int: 1, const int: 2)", _Generic(ci, int: 1, const int: 2));
  return 0;
}
