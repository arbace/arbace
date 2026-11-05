// templates in namespaces: the mangling's prefixes and substitutions, argument-dependent lookup of a function template
#include <cstdio>
namespace geo {
  template <class T> struct Point { T x, y; };
  template <class T> T dot(const Point<T>& a, const Point<T>& b) { return a.x * b.x + a.y * b.y; }
  namespace detail { template <class T> T sq(T t) { return t * t; } }
}
template <class T> T norm2(const geo::Point<T>& p) { return geo::detail::sq(p.x) + geo::detail::sq(p.y); }
int main() {
  geo::Point<int> a = {1, 2}, b = {3, 4};
  geo::Point<double> c = {0.5, 1.5};
  printf("%d %g %d %g\n", dot(a, b), dot(c, c), norm2(b), norm2(c));
  return 0;
}
