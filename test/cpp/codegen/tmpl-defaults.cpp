// default template arguments: types, a value, one depending on an earlier parameter
#include <cstdio>
template <class T, int N = 4, class S = T> struct Vec { T v[N]; S sum() const { S s = 0; for (int i = 0; i < N; i++) s += v[i]; return s; } int n() const { return N; } };
template <class T = int> T make(int x) { return (T)x; }
template <class T, class R = long> R widen(T t) { return t; }
int main() {
  Vec<int> a = {{1, 2, 3, 4}};
  Vec<char, 2, int> b = {{100, 100}};
  printf("%d %d %d %d\n", a.sum(), a.n(), b.sum(), b.n());
  printf("%d %g %ld\n", make(7), make<double>(7), widen(3));
  return 0;
}
