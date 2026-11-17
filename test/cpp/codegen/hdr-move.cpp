// G4: <utility>'s std::move and std::forward from libstdc++'s own definitions (bits/move.h): an xvalue moves, a
// forwarding reference keeps the argument's category, std::move of a scalar binds an rvalue reference
#include <cstdio>
#include <utility>
struct S {
  int v;
  S(int x) : v(x) {}
  S(S&& o) : v(o.v) { o.v = -1; }
  S(const S& o) : v(o.v + 100) {}
};
template <class T> int take(T&& t) { S s(std::forward<T>(t)); return s.v; }
int main() {
  S a(5);
  S b(std::move(a));
  std::printf("%d %d\n", a.v, b.v);
  S c(9);
  std::printf("%d %d\n", take(c), take(S(4)));
  int i = 3;
  int&& r = std::move(i);
  r += 1;
  std::printf("%d %d\n", i, r);
  return 0;
}
