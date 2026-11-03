// synth-c23/expr-postfix.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// postfix expressions: subscripts both ways, calls, members, increments, compound literals
// ISO/IEC 9899:2024 6.5.3; Annex A: postfix-expression, argument-expression-list, compound-literal
#include <stdio.h>

struct pt { int x, y; };
static int add(int a, int b) { return a + b; }
static int none(void) { return 4; }

int main(void) {
  int a[4] = {10, 20, 30, 40};
  struct pt p = {1, 2};
  struct pt *pp = &p;
  int i = 5;
  int post = i++;
  int postd = i--;
  printf("%s = %d\n", "a[1]", a[1]);
  printf("%s = %d\n", "2[a]", 2[a]);
  printf("%s = %d\n", "add(3, 4)", add(3, 4));
  printf("%s = %d\n", "none()", none());
  printf("%s = %d\n", "p.x", p.x);
  printf("%s = %d\n", "pp->y", pp->y);
  printf("%s = %d\n", "post", post);
  printf("%s = %d\n", "postd", postd);
  printf("%s = %d\n", "i", i);
  printf("%s = %d\n", "(int[]){7, 8, 9}[2]", (int[]){7, 8, 9}[2]);
  printf("%s = %d\n", "(struct pt){.y = 6}.y", (struct pt){.y = 6}.y);
  printf("%s = %d\n", "((struct pt *)&p)->x", ((struct pt *)&p)->x);
  printf("%s = %d\n", "(&a[0])[3]", (&a[0])[3]);
  return 0;
}
