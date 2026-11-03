// synth-c23/ext-callbacks.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// function pointers as callbacks: qsort and bsearch with comparators, dispatch tables
// ISO/IEC 9899:2024 6.9.2, 7.24.5; Annex A: function-definition, declarator (pointer to function)
#include <stdio.h>
#include <stdlib.h>

static int cmp(const void *a, const void *b) { int x = *(const int *)a, y = *(const int *)b; return (x > y) - (x < y); }
static int add(int a, int b) { return a + b; }
static int sub(int a, int b) { return a - b; }
static int mul(int a, int b) { return a * b; }
static int (*const ops[])(int, int) = {add, sub, mul};

int main(void) {
  int a[6] = {5, 3, 9, 1, 7, 2};
  qsort(a, 6, sizeof a[0], cmp);
  int key = 7;
  int *hit = bsearch(&key, a, 6, sizeof a[0], cmp);
  printf("%s = %d\n", "a[0]", a[0]);
  printf("%s = %d\n", "a[5]", a[5]);
  printf("%s = %d\n", "(int)(hit - a)", (int)(hit - a));
  printf("%s = %d\n", "ops[0](6, 3)", ops[0](6, 3));
  printf("%s = %d\n", "ops[1](6, 3)", ops[1](6, 3));
  printf("%s = %d\n", "ops[2](6, 3)", ops[2](6, 3));
  printf("%s = %d\n", "(*ops[2])(2, 2)", (*ops[2])(2, 2));
  return 0;
}
