// G5: variadic functions: va_start, va_arg (int, long, double, const char*) as clang's X86_64ABIInfo::EmitVAArg writes
// it (the register save area, then the overflow area: more arguments than registers), va_copy, va_end
#include <cstdarg>
#include <cstdio>
static double sum(int n, ...) {
  va_list ap; va_start(ap, n);
  double s = 0;
  for (int i = 0; i < n; ++i) { if (i % 2) s += va_arg(ap, double); else s += va_arg(ap, int); }
  va_end(ap);
  return s;
}
static void show(const char* f, ...) {
  va_list ap; va_start(ap, f);
  const char* s = va_arg(ap, const char*); long l = va_arg(ap, long);
  va_list c; va_copy(c, ap); int x = va_arg(c, int); va_end(c); va_end(ap);
  printf("%s %s %ld %d\n", f, s, l, x);
}
int main() {
  printf("%g\n", sum(9, 1, 2.5, 3, 4.5, 5, 6.5, 7, 8.5, 9));
  printf("%g\n", sum(20, 1,1.0,1,1.0,1,1.0,1,1.0,1,1.0,1,1.0,1,1.0,1,1.0,1,1.0,1,1.0));
  show("a", "b", 7L, 9);
  return 0;
}
