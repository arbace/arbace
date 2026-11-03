// synth-c23/decl-attributes-positions.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// attributes in every position: declarations, declarators, types, parameters, members, enumerators, labels, statements
// ISO/IEC 9899:2024 6.7.13, 6.7.3.2, 6.7.3.3, 6.8.1; Annex A: attribute-specifier-sequence (in declaration, declarator, struct-or-union-specifier, member-declaration, enumerator, parameter-declaration, statements)
// M.2: attributes
#include <stdio.h>

struct [[maybe_unused]] tagged { [[maybe_unused]] int a; int b [[maybe_unused]]; };
enum [[maybe_unused]] e { E0 [[maybe_unused]], E1 [[deprecated]] = 4 };
static int f([[maybe_unused]] int x, int y [[maybe_unused]]) { return y; }
static int arr [[maybe_unused]] [2] = {1, 2};
int [[gnu::unused]] typed_attr = 3;

int main(void) {
  [[maybe_unused]] int a = 1, b [[maybe_unused]] = 2;
  struct tagged t = {a, b};
  [[maybe_unused]] lbl:
  [[]] t.a++;
  [[]];
  [[maybe_unused]] { a++; }
  printf("%s = %d\n", "t.a + t.b", t.a + t.b);
  printf("%s = %d\n", "E0", E0);
  printf("%s = %d\n", "f(0, 8)", f(0, 8));
  printf("%s = %d\n", "arr[1]", arr[1]);
  printf("%s = %d\n", "typed_attr", typed_attr);
  printf("%s = %d\n", "a", a);
  return 0;
}
