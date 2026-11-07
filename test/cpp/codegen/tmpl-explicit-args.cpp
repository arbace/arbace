// explicit template arguments: all given, some given and the rest deduced, a return type only given explicitly
#include <cstdio>
template <class To, class From> To convert(From f) { return (To)f; }
template <class T> T zero() { return T(); }
template <class T, class U> T scale(U u, int k) { return (T)(u * k); }
int main() {
  printf("%d\n", convert<int>(3.75));
  printf("%g\n", convert<double, int>(5));
  printf("%d %g\n", zero<int>(), zero<double>());
  printf("%ld\n", scale<long>(7, 6));
  return convert<char>(65) == 'A' ? 0 : 1;
}
