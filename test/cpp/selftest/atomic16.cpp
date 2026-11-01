// CP161 (b): a 16-byte std::atomic calls libatomic's __atomic_*_16: every link line has -latomic
#include <atomic>
#include <cstdio>
struct P { long a, b; };
int main() {
  std::atomic<P> x{P{1, 2}};
  P o = x.exchange(P{3, 4});
  P c{3, 4};
  bool ok = x.compare_exchange_strong(c, P{5, 6});
  P r = x.load();
  std::printf("%ld %ld %d %ld %ld\n", o.a, o.b, (int)ok, r.a, r.b);
  return 0;
}
