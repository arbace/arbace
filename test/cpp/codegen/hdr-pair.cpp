// G4: std::pair from <utility> (bits/stl_pair.h): its constructors constrained by requires-clauses (P14 (b): the
// native parser keeps them, the analysis evaluates them as constant expressions: _S_constructible's static constexpr
// member templates over <type_traits>), std::make_pair, the members first and second, a pair of a class
#include <cstdio>
#include <utility>
struct V { int v; V(int x) : v(x) {} };
int main() {
  std::pair<int, double> p(3, 2.5);
  auto q = std::make_pair(7, 'c');
  std::pair<V, int> r(V(4), 5);
  p.first += 1;
  std::printf("%d %g %d %c %d %d\n", p.first, p.second, q.first, q.second, r.first.v, r.second);
  return 0;
}
