// the injected-class-name inside a class template, a member returning its own specialization, operators of a template
#include <cstdio>
template <class T> struct Num {
  T v;
  Num(T x) : v(x) {}
  Num operator+(const Num& o) const { return Num(v + o.v); }
  Num<T>& twice() { v = v * 2; return *this; }
  bool operator<(const Num& o) const { return v < o.v; }
};
template <class T> T smaller(T a, T b) { return b < a ? b : a; }
int main() {
  Num<int> a(3), b(4);
  Num<int> c = a + b;
  c.twice().twice();
  printf("%d %d\n", c.v, smaller(a, b).v);
  return 0;
}
