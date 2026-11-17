// function templates deduced from calls: T from two arguments, from a pointer, from a const reference
#include <cstdio>
template <typename T> T max2(T a, T b) { return a < b ? b : a; }
template <typename T> T deref(const T* p) { return *p; }
template <typename T> const T& larger(const T& a, const T& b) { return a < b ? b : a; }
int main() {
  int x = 7;
  double d = 2.5;
  printf("%d %g %c\n", max2(3, 9), max2(1.5, -0.5), max2('a', 'z'));
  printf("%d %g\n", deref(&x), deref(&d));
  printf("%d %ld\n", larger(4, 2), larger(10L, 20L));
  return 0;
}
