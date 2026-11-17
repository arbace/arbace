// a class template with a constructor, members defined inside the class, and two specializations
#include <cstdio>
template <class T> class Pair {
  T a, b;
public:
  Pair(T x, T y) : a(x), b(y) {}
  T first() const { return a; }
  T second() const { return b; }
  T sum() const { return a + b; }
  void swap() { T t = a; a = b; b = t; }
};
int main() {
  Pair<int> p(3, 4);
  Pair<double> q(1.25, 2.5);
  p.swap();
  printf("%d %d %d\n", p.first(), p.second(), p.sum());
  printf("%g %g\n", q.sum(), q.first());
  return 0;
}
