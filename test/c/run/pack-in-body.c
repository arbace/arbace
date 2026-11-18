/* ci-run verdict: exit 0. #pragma pack(push, n) and pop inside a function body (the Mac VM's report 244, item 4; CS290 (a)): gcc's pragmas are
   tokens, so a pragma in a body lays out the records after it, nested pushes too, and pop restores; sizes and offsets on
   x86-64 by the pragma's rule (a member aligned to the lesser of its alignment and n), as gcc 15.2 gives them (measured
   by hand). The program checks itself (abort on a difference). */
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
static int fails;
#define EQ(x, v) do { if ((size_t)(x) != (size_t)(v)) { printf("FAIL %s: %zu, want %zu\n", #x, (size_t)(x), (size_t)(v)); fails++; } } while (0)
static int f(void) {
#pragma pack(push, 2)
  struct P { char c; int i; double d; };
  struct P p = { 1, 2, 3.0 };
  {
#pragma pack(push, 1)
    struct R { char c; long l; };
    struct R r = { 4, 5 };
    EQ(sizeof(struct R), 9); EQ(offsetof(struct R, l), 1);
    printf("R %zu %zu %d %ld\n", sizeof(struct R), offsetof(struct R, l), r.c, r.l);
#pragma pack(pop)
  }
  struct S { char c; long l; };
#pragma pack(pop)
  struct T { char c; long l; };
  EQ(sizeof(struct P), 14); EQ(offsetof(struct P, i), 2); EQ(offsetof(struct P, d), 6);
  EQ(sizeof(struct S), 10); EQ(sizeof(struct T), 16);
  printf("P %zu %zu %zu %d %d %g S %zu T %zu\n", sizeof(struct P), offsetof(struct P, i), offsetof(struct P, d), p.c, p.i, p.d,
         sizeof(struct S), sizeof(struct T));
  return 0;
}
int main(void) {
#pragma pack(push, 1)
  struct Q { char c; double d; };
#pragma pack(pop)
  struct U { char c; double d; };
  struct Q q[2] = { { 1, 2.5 }, { 3, 4.5 } };
  EQ(sizeof(struct Q), 9); EQ(offsetof(struct Q, d), 1); EQ(sizeof(struct U), 16); EQ(sizeof q, 18);
  printf("Q %zu %zu U %zu %g %g\n", sizeof(struct Q), offsetof(struct Q, d), sizeof(struct U), q[0].d, q[1].d);
  f();
  printf("pack-in-body: %d failing\n", fails);
  if (fails) abort();
  return 0;
}
