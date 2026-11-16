/* GNU's structures and unions with variably modified members (CS106): their layout at run time where the definition
   stands, sizeof, offsetof, members after one of variable size, copies, arrays of them, pointers stepping by them,
   an argument of variable size through varargs. */
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

static void layout(int n, int m) {
  struct S { char c; double d[n]; char e; int f[m]; short g; };
  union U { char a[n]; long b[m]; };
  struct __attribute__((packed)) P { char c; int v[n]; char e; };
  struct N { int k; struct S s; char z; };
  printf("S %zu %zu %zu %zu %zu\n", sizeof(struct S), offsetof(struct S, e), offsetof(struct S, f),
         offsetof(struct S, g), offsetof(struct S, f[2]));
  printf("U %zu P %zu %zu N %zu %zu\n", sizeof(union U), sizeof(struct P), offsetof(struct P, e),
         sizeof(struct N), offsetof(struct N, z));
  struct S a[3], b;
  printf("a %zu %td\n", sizeof a, (char *)&a[2] - (char *)&a[0]);
  for (int i = 0; i < 3; i++) {
    a[i].c = 'a' + i; a[i].e = 'A' + i; a[i].g = 100 + i;
    for (int j = 0; j < n; j++) a[i].d[j] = i * 10 + j;
    for (int j = 0; j < m; j++) a[i].f[j] = i * 100 + j;
  }
  b = a[1];
  struct S *p = a;
  p += 2;
  printf("b %c %c %d %g %d; p %c %d %d\n", b.c, b.e, b.g, b.d[n - 1], b.f[m - 1], p->c, p->g, p->f[1]);
  n = 1; /* the type's sizes were evaluated at its definition */
  printf("still %zu\n", sizeof(struct S));
}

int main(void) {
  int n = 3;
  struct { char pad; int v[n]; short tail; } x = {};
  x.pad = 1; x.tail = 2;
  for (int i = 0; i < n; i++) x.v[i] = 10 * (i + 1);
  printf("sum %ld\n", sum(n, x));
  layout(5, 3);
  layout(1, 7);
  return 0;
}
