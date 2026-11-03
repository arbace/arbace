// synth-c23/expr-nullptr.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// nullptr and nullptr_t: conversions, comparisons, conditions
// ISO/IEC 9899:2024 6.4.4.6, 7.21.2; Annex A: predefined-constant (nullptr)
// M.2: nullptr and nullptr_t
#include <stdio.h>

int main(void) {
  typeof(nullptr) n = nullptr;
  int *p = n;
  void *v = nullptr;
  int (*fp)(void) = nullptr;
  int x = 0;
  int *q = &x;
  printf("%s = %d\n", "p == nullptr", p == nullptr);
  printf("%s = %d\n", "v == nullptr", v == nullptr);
  printf("%s = %d\n", "fp == nullptr", fp == nullptr);
  printf("%s = %d\n", "q != nullptr", q != nullptr);
  printf("%s = %d\n", "nullptr == n", nullptr == n);
  printf("%s = %d\n", "n ? 1 : 0", n ? 1 : 0);
  printf("%s = %d\n", "!nullptr", !nullptr);
  printf("%s = %d\n", "_Generic(nullptr, typeof(nullptr): 1, default: 0)", _Generic(nullptr, typeof(nullptr): 1, default: 0));
  printf("%s = %d\n", "_Generic(n, void *: 1, default: 0)", _Generic(n, void *: 1, default: 0));
  printf("%s = %d\n", "sizeof(nullptr) == sizeof(void *)", sizeof(nullptr) == sizeof(void *));
  printf("%s = %d\n", "(p ? 2 : 3)", (p ? 2 : 3));
  return 0;
}
