// non-type template parameters: an int used in an expression, an array bound, a bool, a negative value
#include <cstdio>
template <int N> int times(int x) { return N * x; }
template <int N> struct Buf { int v[N]; int size() const { return N; } int sum() const { int s = 0; for (int i = 0; i < N; i++) s += v[i]; return s; } };
template <bool B, int N> int pick() { return B ? N : -N; }
int main() {
  Buf<4> b = {{1, 2, 3, 4}};
  printf("%d %d\n", times<3>(5), times<-2>(5));
  printf("%d %d %d\n", b.size(), b.sum(), (int)sizeof(Buf<10>));
  printf("%d %d\n", pick<true, 3>(), pick<false, 3>());
  return 0;
}
