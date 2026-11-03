// synth-c23/decl-abstract-declarators.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// type names with abstract declarators in casts, sizeof, compound literals and _Generic
// ISO/IEC 9899:2024 6.7.8; Annex A: type-name, abstract-declarator, direct-abstract-declarator, array-abstract-declarator, function-abstract-declarator
#include <stdio.h>

static int inc(int x) { return x + 1; }

int main(void) {
  int a[4] = {1, 2, 3, 4};
  int (*pa)[4] = (int (*)[4])&a;
  int (*f)(int) = (int (*)(int))inc;
  void *v = (void *)&a;
  printf("%s = %d\n", "(*pa)[3]", (*pa)[3]);
  printf("%s = %d\n", "f(4)", f(4));
  printf("%s = %zu\n", "sizeof(int[4]) / sizeof(int)", sizeof(int[4]) / sizeof(int));
  printf("%s = %d\n", "sizeof(int (*)[4]) == sizeof(void *)", sizeof(int (*)[4]) == sizeof(void *));
  printf("%s = %d\n", "_Generic(&a, int (*)[4]: 1, default: 0)", _Generic(&a, int (*)[4]: 1, default: 0));
  printf("%s = %d\n", "_Generic(inc, int (*)(int): 1, default: 0)", _Generic(inc, int (*)(int): 1, default: 0));
  printf("%s = %d\n", "((int *)v)[2]", ((int *)v)[2]);
  printf("%s = %d\n", "_Generic((int *const *)0, int *const *: 1, default: 0)", _Generic((int *const *)0, int *const *: 1, default: 0));
  printf("%s = %d\n", "(int){(int)sizeof(char[5])}", (int){(int)sizeof(char[5])});
  return 0;
}
