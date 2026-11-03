// synth-c23/expr-conditional-comma.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// conditional and comma operators, a conditional of structures and unions
// ISO/IEC 9899:2024 6.5.16, 6.5.18; Annex A: conditional-expression, expression
#include <stdio.h>

struct pr { int x, y; };
union un { int i; float f; };
static int made;
static struct pr mk(int x) { made++; return (struct pr){x, x + 1}; }

int main(void) {
  int a = 1, b = 2, c;
  c = (a++, b++, a + b);
  int *p = a > 5 ? &a : &b;
  const char *s = c ? "yes" : "no";
  struct pr pa = {1, 2}, pb = {3, 4};
  struct pr pc = c > 4 ? pa : pb;
  struct pr pm = c ? mk(10) : mk(20);
  union un ua = {.i = 7}, ub = {.i = 8};
  union un uc = c ? ua : ub;
  printf("%s = %d\n", "c", c);
  printf("%s = %d\n", "a", a);
  printf("%s = %d\n", "b", b);
  printf("%s = %d\n", "*p", *p);
  printf("%s = %d\n", "a ? b : c", a ? b : c);
  printf("%s = %d\n", "0 ? 1 : 2 ? 3 : 4", 0 ? 1 : 2 ? 3 : 4);
  printf("%s = %s\n", "s", s);
  printf("%s = %g\n", "1 ? 1 : 2.5", 1 ? 1 : 2.5);
  printf("%s = %d\n", "pc.x", pc.x);
  printf("%s = %d\n", "pm.y", pm.y);
  printf("%s = %d\n", "made", made);
  printf("%s = %d\n", "(c < 0 ? pa : pb).y", (c < 0 ? pa : pb).y);
  printf("%s = %d\n", "uc.i", uc.i);
  return 0;
}
