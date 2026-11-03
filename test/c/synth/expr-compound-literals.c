// synth-c23/expr-compound-literals.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// compound literals at file and block scope, lvalues, C23's storage-class specifiers in them
// ISO/IEC 9899:2024 6.5.3.6; Annex A: compound-literal, storage-class-specifiers
// M.2: storage-class specifiers in compound literals (static, constexpr, register, thread_local)
#include <stdio.h>
#include <string.h>

struct pt { int x, y; };
static int *fp = (int[]){4, 5, 6};
static int *counter(void) { return &(static int){0}; }

int main(void) {
  int *bp = (int[3]){1, 2, 3};
  bp[1] = 20;
  ++*counter();
  ++*counter();
  struct pt *q = &(struct pt){.x = 3, .y = 4};
  q->y += 1;
  int *ai = &(int){5};
  *ai += 1;
  double *ad = &(double){2.5};
  int (*ar)[2] = &(int[2]){8, 9};
  long *sl = &(static long){-3};
  unsigned mx = 0;
  memcpy(&mx, &(unsigned){7u}, sizeof mx);
  printf("%s = %u\n", "mx", mx);
  printf("%s = %d\n", "fp[2]", fp[2]);
  printf("%s = %d\n", "bp[1]", bp[1]);
  printf("%s = %d\n", "*counter()", *counter());
  printf("%s = %d\n", "q->x + q->y", q->x + q->y);
  printf("%s = %d\n", "(constexpr int){42}", (constexpr int){42});
  printf("%s = %d\n", "(register int){7} + 1", (register int){7} + 1);
  printf("%s = %d\n", "(const int){3}", (const int){3});
  printf("%s = %d\n", "*ai", *ai);
  printf("%s = %g\n", "*ad", *ad);
  printf("%s = %d\n", "(*ar)[1]", (*ar)[1]);
  printf("%s = %ld\n", "*sl", *sl);
  return 0;
}
