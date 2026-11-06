// cppi-deferred: three of cppi's deferred classes closed in the lowering (doc/cpp/LLVM.md §7, §11). A struct that holds a long
// double (std::format's argument, a std::pair, an array) in its slot union, so ci's check takes it; atomics of float and double
// (load, store, exchange, fetch_add's compare-exchange loop) by the _n builtins on their bits; a variadic function that reads its
// va_list inline (ints, doubles, a small and a large struct, a pointer), a va_list parameter, va_copy, and a va_list read and then
// passed on to vsnprintf. Long double values are ones a double holds exactly (ci computes long double as double, CS35).
#include <atomic>
#include <cstdarg>
#include <cstdio>
#include <format>
#include <functional>
#include <utility>
struct P { int a, b; };
struct Big { long x[3]; };
struct LD { char c; long double v; int tail; };
static long vsum(int n, va_list ap) { long s = 0; for (int i = 0; i < n; i++) s += va_arg(ap, int); return s; }
static long sum(int n, ...) { va_list ap; va_start(ap, n); long s = vsum(n, ap); va_end(ap); return s; }
static double mix(int n, ...) {
  va_list ap; va_start(ap, n); double s = 0;
  for (int i = 0; i < n; i++) {
    s += va_arg(ap, double); s += va_arg(ap, long); P p = va_arg(ap, P); s += p.a * 10 + p.b;
    Big b = va_arg(ap, Big); s += b.x[2]; const char *c = va_arg(ap, const char *); s += c[0];
  }
  va_list aq; va_copy(aq, ap); va_end(aq); va_end(ap); return s;
}
static int fwd(const char *f, ...) {
  va_list ap; va_start(ap, f); char b[64]; int k = va_arg(ap, int); int r = vsnprintf(b, 64, f, ap); va_end(ap);
  std::printf("[%d %s]\n", k, b); return r;
}
int main() {
  std::printf("%ld\n", sum(5, 1, 2, 3, 4, 5));
  std::printf("%g\n", mix(2, 1.5, 7L, P{1, 2}, Big{{0, 0, 9}}, "A", 2.5, 8L, P{3, 4}, Big{{0, 0, 11}}, "B"));
  fwd("%d-%s", 42, 7, "x");
  std::atomic<float> f{1.5f}; std::atomic<double> d{2.25};
  f.store(3.5f); float o = f.exchange(4.0f); float a = f.fetch_add(0.5f);
  double e = d.exchange(8.5); d.fetch_sub(1.25); double exp = d.load(); bool ok = d.compare_exchange_strong(exp, 1.0);
  std::printf("%g %g %g | %g %g %d %g\n", o, a, f.load(), e, exp, ok, d.load());
  LD ld[2] = {{'a', 1.5L, 7}, {'b', -0.25L, 9}};
  std::pair<long double, int> pr{ld[1].v * 4, ld[0].tail};
  std::printf("%d %d %.2f %d %c%c\n", int(sizeof(LD)), int(alignof(LD)), double(ld[0].v + pr.first), pr.second, ld[0].c, ld[1].c);
  std::printf("%s\n", std::format("{} {} {:.3f}", 2.5, 'q', double(0.125L)).c_str());
  std::printf("%d\n", std::hash<long double>{}(0.0L) == std::hash<long double>{}(-0.0L));
  return 0;
}
