// deduction through a class template's specialization in a parameter, and from a derived class
#include <cstdio>
template <class T> struct Wrap { T v; };
template <class T> T unwrap(const Wrap<T>& w) { return w.v; }
template <class T> T* addr(Wrap<T>& w) { return &w.v; }
struct Derived : Wrap<long> { int extra; };
int main() {
  Wrap<int> a = {3};
  Wrap<double> b = {1.5};
  Derived d; d.v = 99; d.extra = 1;
  *addr(a) = 30;
  printf("%d %g %ld\n", unwrap(a), unwrap(b), unwrap(d));
  return 0;
}
