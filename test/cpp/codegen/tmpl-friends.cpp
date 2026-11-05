// friends defined in class templates (hidden friends found by argument-dependent lookup, a friend function template),
// a friend of a plain class, and classes local to a function template (Z <encoding> E names)
#include <cstdio>
template <class T> struct Num {
  T v;
  friend Num operator+(Num a, Num b) { return Num{a.v + b.v}; }
  friend T get(const Num& n) { return n.v; }
  template <class U> friend U conv(const Num& n) { return (U)n.v; }
};
struct P { int x; friend int twice(P p) { return p.x * 2; } };
template <int I> int local() {
  enum E { a = I };
  struct S { int g() const { E e = a; return (int)e * 10; } };
  S s;
  return s.g();
}
int main() {
  Num<int> a{1}, b{2};
  Num<int> c = a + b;
  Num<double> d{2.5};
  P p{4};
  printf("%d %g %d %d %d %d %d\n", get(c), get(d + d), twice(p), conv<int>(c), conv<int>(d), local<3>(), local<-1>());
  return 0;
}
