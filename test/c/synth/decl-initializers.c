// synth-c23/decl-initializers.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// initializers: scalars, braces, nesting, designators, strings, unions, overrides
// ISO/IEC 9899:2024 6.7.11; Annex A: initializer, braced-initializer, initializer-list, designation, designator-list, designator
#include <stdio.h>

struct pt { int x, y; };
struct line { struct pt a, b; int tag[2]; };
union iu { int i; double d; };

int main(void) {
  int s = {5};
  int a[5] = {1, 2};
  int d[6] = {[4] = 4, [1] = 1, 2};
  struct pt p = {.y = 2, .x = 1};
  struct line l = {.b.y = 7, .tag[1] = 9, .a = {3, 4}};
  struct line l2 = {1, 2, 3, 4, 5, 6};
  char str[] = "abc";
  char exact[3] = "xyz";
  union iu u1 = {7};
  union iu u2 = {.d = 2.5};
  int over[3] = {[0] = 1, [0] = 2};
  int m[2][2] = {1, 2, 3};
  struct pt ps[] = {{1, 2}, [2] = {.y = 5}};
  printf("%s = %d\n", "s", s);
  printf("%s = %d\n", "a[1] + a[4]", a[1] + a[4]);
  printf("%s = %d\n", "d[1] + d[2] + d[4]", d[1] + d[2] + d[4]);
  printf("%s = %d\n", "p.x * 10 + p.y", p.x * 10 + p.y);
  printf("%s = %d\n", "l.a.y + l.b.y + l.tag[1] + l.b.x", l.a.y + l.b.y + l.tag[1] + l.b.x);
  printf("%s = %d\n", "l2.tag[1]", l2.tag[1]);
  printf("%s = %zu\n", "sizeof str", sizeof str);
  printf("%s = %d\n", "exact[2]", exact[2]);
  printf("%s = %d\n", "u1.i", u1.i);
  printf("%s = %g\n", "u2.d", u2.d);
  printf("%s = %d\n", "over[0]", over[0]);
  printf("%s = %d\n", "m[1][0] + m[1][1]", m[1][0] + m[1][1]);
  printf("%s = %zu\n", "sizeof ps / sizeof ps[0]", sizeof ps / sizeof ps[0]);
  printf("%s = %d\n", "ps[2].y", ps[2].y);
  return 0;
}
