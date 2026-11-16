// classes through an ellipsis ([expr.call]/12, conditionally-supported; clang passes a trivially copyable one as a parameter of its
// type, the psABI's classes, the registers counted on; cpp-route) and va_arg of them (the register save area, a mixed or two-SSE
// class assembled in a temporary, a MEMORY class from the overflow area)
#include <cstdarg>
#include <cstdio>
struct P { int a, b; };
struct L { long x, y; };
struct M { double d; long l; };
struct F { double x, y; };
struct Big { long v[3]; };
static void show(int n, ...) {
  va_list ap; va_start(ap, n);
  for (int i = 0; i < n; i++) {
    int k = va_arg(ap, int);
    switch (k) {
      case 0: { P p = va_arg(ap, P); std::printf("P %d %d\n", p.a, p.b); break; }
      case 1: { L l = va_arg(ap, L); std::printf("L %ld %ld\n", l.x, l.y); break; }
      case 2: { M m = va_arg(ap, M); std::printf("M %.2f %ld\n", m.d, m.l); break; }
      case 3: { F f = va_arg(ap, F); std::printf("F %.2f %.2f\n", f.x, f.y); break; }
      case 4: { Big b = va_arg(ap, Big); std::printf("B %ld %ld %ld\n", b.v[0], b.v[1], b.v[2]); break; }
    }
  }
  va_end(ap);
}
int main() {
  show(5, 0, P{1, 2}, 1, L{3, 4}, 2, M{5.5, 6}, 3, F{7.25, 8.5}, 4, Big{{9, 10, 11}});
  // enough to spill every kind to the overflow area
  show(9, 2, M{1.5, 1}, 3, F{2.5, 3.5}, 3, F{4.5, 5.5}, 3, F{6.5, 7.5}, 3, F{8.5, 9.5}, 1, L{12, 13}, 1, L{14, 15}, 0, P{16, 17}, 2, M{18.5, 19});
  return 0;
}
