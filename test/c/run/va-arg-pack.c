/* ci-run verdict: exit 0. __builtin_va_arg_pack and __builtin_va_arg_pack_len (CS277): always_inline variadic functions forwarding their
   variadic arguments to variadic callees, integers, doubles and strings, none, and printf. Checks itself: a value other than
   the forwarded arguments' returns 1. */
#include <stdio.h>
#include <stdarg.h>
static int sum(int n, ...) { va_list ap; va_start(ap, n); int t = 0; for (int i = 0; i < n; i++) t += va_arg(ap, int); va_end(ap); return t; }
static double dsum(const char *f, ...) { va_list ap; va_start(ap, f); double t = 0; for (; *f; f++) t += *f == 'd' ? va_arg(ap, double) : va_arg(ap, int); va_end(ap); return t; }
static inline __attribute__((always_inline)) int fwd(int n, ...) { return sum(n, __builtin_va_arg_pack()); }
static inline __attribute__((always_inline)) int cnt(int n, ...) { return __builtin_va_arg_pack_len(); }
static inline __attribute__((always_inline)) double dfwd(const char *f, ...) { return dsum(f, __builtin_va_arg_pack()); }
static inline __attribute__((always_inline)) int pr(const char *f, ...) { return printf(f, __builtin_va_arg_pack()); }
int main(void) {
  int s = fwd(3, 1, 2, 3), k = cnt(0, 1, 2, 3, 4), z = fwd(0);
  double d = dfwd("did", 1.5, 2, 3.25);
  printf("%d %d %d\n", s, k, z);
  printf("%g\n", d);
  int m = pr("%s %d %.2f\n", "pack", 42, 2.5);
  return s == 6 && k == 4 && z == 0 && d == 6.75 && m == 13 ? 0 : 1;
}
