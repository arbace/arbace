// names that depend on the parameter: members of T, T's nested types and constants, sizeof(T), T() value-initialized
#include <cstdio>
struct A { typedef int value_type; static const int K = 3; int v; int get() const { return v; } };
struct B { typedef double value_type; static const int K = 5; double v; double get() const { return v * 2; } };
template <class T> typename T::value_type twice(const T& t) { typename T::value_type r = t.get(); return r + r; }
template <class T> int info() { T t = T(); return T::K * 100 + (int)sizeof(T) + (int)t.v; }
int main() {
  A a = {4}; B b = {1.5};
  printf("%d %g %d %d\n", twice(a), twice(b), info<A>(), info<B>());
  return 0;
}
