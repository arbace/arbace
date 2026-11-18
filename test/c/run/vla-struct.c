/* ci-run verdict: exit 0. GNU's structures and unions with variably modified members (CS106): their layout at run time where the definition
   stands, sizeof, offsetof, members after one of variable size, copies, arrays of them, pointers stepping by them,
   an argument of variable size through varargs. Checks itself: each size and offset against the x86-64 psABI's layout
   of the members at the run's n and m, and the values stored, worked out by hand; a difference returns 1. */
#include <stdarg.h>
#include <stdio.h>
#include <stddef.h>
#include <string.h>

static long sum(int n, ...) {
  struct { char pad; int v[n]; short tail; } s;
  va_list ap;
  va_start(ap, n);
  s = va_arg(ap, typeof(s));
  va_end(ap);
  long t = s.pad + s.tail;
  for (int i = 0; i < n; i++) t += s.v[i];
  return t;
}

static int fails;
#define EQ(x, v) do { if ((long)(x) != (long)(v)) { fprintf(stderr, "FAIL %s: %ld, want %ld\n", #x, (long)(x), (long)(v)); fails++; } } while (0)
/* W: n, m and the sizes and offsets the layout gives (char c at 0; d[n] at 8; e after it; f[m] at e's next multiple of 4;
   g after f; the size to 8; U the larger of n and 8m to 8; P packed 1 + 4n + 1; N's s at 8, z after it, its size to 8) */
struct W { int n, m; long s, e, f, g, f2, u, p, pe, nsz, nz; };
static const struct W ws[] = { { 5, 3, 72, 48, 52, 64, 60, 24, 22, 21, 88, 80 }, { 1, 7, 56, 16, 20, 48, 28, 56, 6, 5, 72, 64 } };
static void layout(int n, int m) {
  const struct W *w = n == 5 ? &ws[0] : &ws[1];
  struct S { char c; double d[n]; char e; int f[m]; short g; };
  union U { char a[n]; long b[m]; };
  struct __attribute__((packed)) P { char c; int v[n]; char e; };
  struct N { int k; struct S s; char z; };
  printf("S %zu %zu %zu %zu %zu\n", sizeof(struct S), offsetof(struct S, e), offsetof(struct S, f),
         offsetof(struct S, g), offsetof(struct S, f[2]));
  printf("U %zu P %zu %zu N %zu %zu\n", sizeof(union U), sizeof(struct P), offsetof(struct P, e),
         sizeof(struct N), offsetof(struct N, z));
  EQ(w->n, n); EQ(w->m, m); EQ(sizeof(struct S), w->s); EQ(offsetof(struct S, e), w->e); EQ(offsetof(struct S, f), w->f);
  EQ(offsetof(struct S, g), w->g); EQ(offsetof(struct S, f[2]), w->f2); EQ(sizeof(union U), w->u); EQ(sizeof(struct P), w->p);
  EQ(offsetof(struct P, e), w->pe); EQ(sizeof(struct N), w->nsz); EQ(offsetof(struct N, z), w->nz);
  struct S a[3], b;
  printf("a %zu %td\n", sizeof a, (char *)&a[2] - (char *)&a[0]);
  EQ(sizeof a, 3 * w->s); EQ((char *)&a[2] - (char *)&a[0], 2 * w->s);
  for (int i = 0; i < 3; i++) {
    a[i].c = 'a' + i; a[i].e = 'A' + i; a[i].g = 100 + i;
    for (int j = 0; j < n; j++) a[i].d[j] = i * 10 + j;
    for (int j = 0; j < m; j++) a[i].f[j] = i * 100 + j;
  }
  b = a[1];
  struct S *p = a;
  p += 2;
  printf("b %c %c %d %g %d; p %c %d %d\n", b.c, b.e, b.g, b.d[n - 1], b.f[m - 1], p->c, p->g, p->f[1]);
  EQ(b.c, 'b'); EQ(b.e, 'B'); EQ(b.g, 101); EQ(b.d[n - 1], 10 + n - 1); EQ(b.f[m - 1], 100 + m - 1); EQ(p->c, 'c'); EQ(p->g, 102); EQ(p->f[1], 201);
  n = 1; /* the type's sizes were evaluated at its definition */
  printf("still %zu\n", sizeof(struct S));
  EQ(sizeof(struct S), w->s);
}

int main(void) {
  int n = 3;
  struct { char pad; int v[n]; short tail; } x = {};
  x.pad = 1; x.tail = 2;
  for (int i = 0; i < n; i++) x.v[i] = 10 * (i + 1);
  long t = sum(n, x);
  printf("sum %ld\n", t);
  EQ(t, 1 + 2 + 10 + 20 + 30);
  layout(5, 3);
  layout(1, 7);
  return fails ? 1 : 0;
}
