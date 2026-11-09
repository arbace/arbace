// structured bindings ([dcl.struct.bind]; cpp-route): of an array (copied element by element without a ref-qualifier, the
// elements themselves with one), of a class's data members (a base's too), of a tuple-like type (std::pair, std::tuple: get<i>
// by argument-dependent lookup, the bindings' variables of reference type), in a range-based for
#include <cstdio>
#include <utility>
#include <tuple>
#include <map>
struct S { int a; long b; double c; };
struct B { int x, y; };
struct D : B {};
int main() {
  int arr[3] = {7, 8, 9};
  auto [i, j, k] = arr;
  auto& [ri, rj, rk] = arr;
  ri = 70; i += 1;
  std::printf("%d %d %d | %d %d %d\n", i, j, k, arr[0], arr[1], arr[2]);
  S s{1, 2, 3.5};
  auto [m, n, o] = s;
  auto& [rm, rn, ro] = s;
  rm = 10; m = 5;
  std::printf("%d %ld %.1f | %d %ld %.1f | %d\n", m, n, o, rm, rn, ro, s.a);
  D d; d.x = 3; d.y = 4;
  const auto& [dx, dy] = d;
  std::printf("%d %d\n", dx, dy);
  std::pair<int, long> p(1, 2);
  auto [x, y] = p;
  auto& [rx, ry] = p;
  rx = 5;
  std::printf("%d %ld %d %ld %d\n", x, y, rx, ry, p.first);
  std::tuple<int, char, long> t(4, 'z', 6);
  auto [t0, t1, t2] = t;
  std::printf("%d %c %ld\n", t0, t1, t2);
  S ss[2] = {{1, 2, 0.5}, {3, 4, 1.5}};
  double sum = 0;
  for (auto& [u, v, w] : ss) { sum += u * 10 + v + w; u = 0; }
  std::printf("%.1f %d %d\n", sum, ss[0].a, ss[1].a);
  std::map<int, int> mp; mp[2] = 20; mp[1] = 10;
  for (const auto& [key, val] : mp) std::printf("%d=%d\n", key, val);
  return 0;
}
