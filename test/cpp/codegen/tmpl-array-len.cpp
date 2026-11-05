// deduction of a type and an array bound from a reference to an array
#include <cstdio>
template <class T, unsigned long N> unsigned long count(T (&)[N]) { return N; }
template <class T, int N> T total(const T (&a)[N]) { T s = 0; for (int i = 0; i < N; i++) s += a[i]; return s; }
int main() {
  int a[5] = {1, 2, 3, 4, 5};
  double d[3] = {0.5, 1.5, 2.0};
  char s[] = "hello";
  printf("%lu %lu %lu\n", count(a), count(d), count(s));
  printf("%d %g\n", total(a), total(d));
  return 0;
}
