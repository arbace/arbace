// G5c: a cycle of using-directives ([namespace.udir]/2-3: the nominated sets' union, each namespace once): libsupc++'s
// tinfo.h, an unnamed namespace inside __cxxabiv1 nominating abi = __cxxabiv1; and GNU's restrict-qualified reference
// parameters (`T &__restrict r`: its __do_upcast)
#include <cstdio>
namespace outer {
  int f(int x) { return x + 1; }
  struct R { int v; };
}
namespace al = outer;
namespace outer {
  namespace { using namespace al; int g(int x) { return f(x) * 2; } }
  void set(R &__restrict r, int v) { r.v = v; }
}
using namespace outer;
int main() {
  R r; set(r, g(4));
  printf("%d %d\n", r.v, f(1));
  return 0;
}
