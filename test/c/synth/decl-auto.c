// synth-c23/decl-auto.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// C23's auto type inference: from constants, expressions, pointers, in for loops
// ISO/IEC 9899:2024 6.7.10; Annex A: storage-class-specifier (auto), type inference
// M.2: auto for type inference
#include <stdio.h>

int main(void) {
  auto i = 1;
  auto d = 1.5;
  auto u = 2u;
  auto l = 3ll;
  auto p = &i;
  auto s = "str";
  auto c = 'c';
  auto sum = i + d;
  int arr[3] = {1, 2, 3};
  auto ap = arr;
  int acc = 0;
  for (auto k = 0; k < 3; k++) acc += ap[k];
  static auto si = 7;
  const auto ci = 8;
  printf("%s = %d\n", "_Generic(i, int: 1, default: 0)", _Generic(i, int: 1, default: 0));
  printf("%s = %d\n", "_Generic(d, double: 1, default: 0)", _Generic(d, double: 1, default: 0));
  printf("%s = %d\n", "_Generic(u, unsigned: 1, default: 0)", _Generic(u, unsigned: 1, default: 0));
  printf("%s = %d\n", "_Generic(l, long long: 1, default: 0)", _Generic(l, long long: 1, default: 0));
  printf("%s = %d\n", "_Generic(p, int *: 1, default: 0)", _Generic(p, int *: 1, default: 0));
  printf("%s = %d\n", "_Generic(s, char *: 1, default: 0)", _Generic(s, char *: 1, default: 0));
  printf("%s = %d\n", "_Generic(c, int: 1, default: 0)", _Generic(c, int: 1, default: 0));
  printf("%s = %d\n", "_Generic(sum, double: 1, default: 0)", _Generic(sum, double: 1, default: 0));
  printf("%s = %d\n", "_Generic(ap, int *: 1, default: 0)", _Generic(ap, int *: 1, default: 0));
  printf("%s = %d\n", "acc", acc);
  printf("%s = %d\n", "si", si);
  printf("%s = %d\n", "ci", ci);
  printf("%s = %d\n", "*p", *p);
  return 0;
}
