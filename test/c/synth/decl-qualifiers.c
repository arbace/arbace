// synth-c23/decl-qualifiers.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// type qualifiers: const, volatile, restrict, _Atomic, on objects and pointers
// ISO/IEC 9899:2024 6.7.4; Annex A: type-qualifier, type-qualifier-list
#include <stdio.h>

static int copy(int *restrict dst, const int *restrict src, int n) { for (int i = 0; i < n; i++) dst[i] = src[i]; return dst[n - 1]; }

int main(void) {
  const int c = 1;
  volatile int v = 2;
  v = v + 1;
  const volatile int cv = 4;
  int src[3] = {5, 6, 7}, dst[3];
  int x = 8;
  int *const cp = &x;
  *cp = 9;
  const int *pc = &c;
  _Atomic int at = 10;
  at += 1;
  _Atomic(long) al = 12;
  printf("%s = %d\n", "c", c);
  printf("%s = %d\n", "v", v);
  printf("%s = %d\n", "cv", cv);
  printf("%s = %d\n", "copy(dst, src, 3)", copy(dst, src, 3));
  printf("%s = %d\n", "x", x);
  printf("%s = %d\n", "*pc", *pc);
  printf("%s = %d\n", "at", at);
  printf("%s = %ld\n", "(long)al", (long)al);
  printf("%s = %d\n", "_Generic(&c, const int *: 1, default: 0)", _Generic(&c, const int *: 1, default: 0));
  return 0;
}
