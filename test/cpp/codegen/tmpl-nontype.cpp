// non-type template parameters: an int used in an expression, an array bound, a bool, a negative value; a function
// pointer (with and without &), a pointer and a reference to a variable, a null pointer
#include <cstdio>
template <int N> int times(int x) { return N * x; }
template <int N> struct Buf { int v[N]; int size() const { return N; } int sum() const { int s = 0; for (int i = 0; i < N; i++) s += v[i]; return s; } };
template <bool B, int N> int pick() { return B ? N : -N; }
int inc(int x) { return x + 1; }
int dbl(int x) { return x * 2; }
int gv = 40;
template <int (*F)(int)> int apply(int x) { return F(x) + (*F)(x); }
template <int* P> int read() { return P ? *P : -1; }
template <int& R> void bump() { R++; }
template <int (*F)(int)> struct Op { int run(int x) const { return F(x); } };
int main() {
  Buf<4> b = {{1, 2, 3, 4}};
  printf("%d %d\n", times<3>(5), times<-2>(5));
  printf("%d %d %d\n", b.size(), b.sum(), (int)sizeof(Buf<10>));
  printf("%d %d\n", pick<true, 3>(), pick<false, 3>());
  bump<gv>();
  Op<dbl> op;
  printf("%d %d %d %d %d\n", apply<&inc>(1), apply<dbl>(5), read<&gv>(), read<nullptr>(), op.run(21));
  return 0;
}
