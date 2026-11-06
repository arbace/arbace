/* __builtin_va_arg_pack and __builtin_va_arg_pack_len (CS277): always_inline variadic functions forwarding their
   variadic arguments to variadic callees, integers, doubles and strings, none, and printf. */
#include <stdio.h>
#include <stdarg.h>
static int sum(int n, ...) { va_list ap; va_start(ap, n); int t = 0; for (int i = 0; i < n; i++) t += va_arg(ap, int); va_end(ap); return t; }
static double dsum(const char *f, ...) { va_list ap; va_start(ap, f); double t = 0; for (; *f; f++) t += *f == 'd' ? va_arg(ap, double) : va_arg(ap, int); va_end(ap); return t; }
static inline __attribute__((always_inline)) int fwd(int n, ...) { return sum(n, __builtin_va_arg_pack()); }
static inline __attribute__((always_inline)) int cnt(int n, ...) { return __builtin_va_arg_pack_len(); }
static inline __attribute__((always_inline)) double dfwd(const char *f, ...) { return dsum(f, __builtin_va_arg_pack()); }
static inline __attribute__((always_inline)) int pr(const char *f, ...) { return printf(f, __builtin_va_arg_pack()); }
int main(void) {
  printf("%d %d %d\n", fwd(3, 1, 2, 3), cnt(0, 1, 2, 3, 4), fwd(0));
  printf("%g\n", dfwd("did", 1.5, 2, 3.25));
  pr("%s %d %.2f\n", "pack", 42, 2.5);
  return 0;
}
