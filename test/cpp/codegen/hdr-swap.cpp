// G4: std::swap from <utility>: its constraint enable_if<__and_<__not_<__is_tuple_like<T>>, is_move_constructible<T>,
// is_move_assignable<T>>::value> (decltype of an overloaded call, the type-trait builtins), the declaration in
// <type_traits> and the definition in <bits/move.h> one template; ints, a class
#include <cstdio>
#include <utility>
struct P { int a, b; };
struct M {
  int v;
  M(int x) : v(x) {}
  M(M&& o) : v(o.v) { o.v = 0; }
  M& operator=(M&& o) { v = o.v; o.v = 0; return *this; }
};
int main() {
  int x = 1, y = 2;
  std::swap(x, y);
  P p{1, 2}, q{3, 4};
  std::swap(p, q);
  M m(10), n(20);
  std::swap(m, n);
  std::printf("%d %d %d %d %d %d\n", x, y, p.a, q.b, m.v, n.v);
  return 0;
}
