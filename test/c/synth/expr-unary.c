// synth-c23/expr-unary.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// unary operators, sizeof and alignof
// ISO/IEC 9899:2024 6.5.4; Annex A: unary-expression, unary-operator
#include <stdio.h>

int main(void) {
  int x = 5;
  int pre = ++x;
  int pred = --x;
  int *p = &x;
  int a[6];
  unsigned u = 0;
  printf("%s = %d\n", "pre", pre);
  printf("%s = %d\n", "pred", pred);
  printf("%s = %d\n", "*p", *p);
  printf("%s = %d\n", "+x", +x);
  printf("%s = %d\n", "-x", -x);
  printf("%s = %d\n", "~x", ~x);
  printf("%s = %d\n", "!x", !x);
  printf("%s = %d\n", "!0", !0);
  printf("%s = %u\n", "~u", ~u);
  printf("%s = %zu\n", "sizeof x", sizeof x);
  printf("%s = %zu\n", "sizeof(int)", sizeof(int));
  printf("%s = %zu\n", "sizeof a / sizeof a[0]", sizeof a / sizeof a[0]);
  printf("%s = %zu\n", "sizeof(char)", sizeof(char));
  printf("%s = %zu\n", "alignof(char)", alignof(char));
  printf("%s = %zu\n", "_Alignof(unsigned char)", _Alignof(unsigned char));
  printf("%s = %zu\n", "sizeof(int[3]) / sizeof(int)", sizeof(int[3]) / sizeof(int));
  printf("%s = %d\n", "*&*p", *&*p);
  return 0;
}
