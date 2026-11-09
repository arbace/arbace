// an aggregate's default member initializers ([dcl.init.aggr]/5; cpp-route): in a list's place (the native parser's
// CXXDefaultInitExpr) and reading another member through `this`, the aggregate being initialized; value-initialized fills
// (ImplicitValueInitExpr); a braced list bound to a const reference ([dcl.init.list]/3.10)
#include <cstdio>
struct X { unsigned i; unsigned n = i + 1; unsigned m = i * 2; };
struct Y { int a; int b[3]; int c = 7; };
static unsigned sum(const X& x) { return x.i + x.n + x.m; }
static int get(const int& r) { return r + 1; }
int main() {
  X x{4};
  X y{1, 9};
  std::printf("%u %u %u | %u %u %u | %u\n", x.i, x.n, x.m, y.i, y.n, y.m, sum(X{2}));
  Y z{1, {2}};
  std::printf("%d %d %d %d %d\n", z.a, z.b[0], z.b[1], z.b[2], z.c);
  std::printf("%d %d\n", get({}), get({41}));
  return 0;
}
