// <utility>'s shapes written out: remove_reference, move, forward, pair, make_pair and swap in a namespace of their own
#include <cstdio>
namespace u {
  template <class T> struct remove_reference { typedef T type; };
  template <class T> struct remove_reference<T&> { typedef T type; };
  template <class T> struct remove_reference<T&&> { typedef T type; };
  template <class T> typename remove_reference<T>::type&& move(T&& t) { return static_cast<typename remove_reference<T>::type&&>(t); }
  template <class T> T&& forward(typename remove_reference<T>::type& t) { return static_cast<T&&>(t); }
  template <class A, class B> struct pair {
    A first; B second;
    pair(const A& a, const B& b) : first(a), second(b) {}
  };
  template <class A, class B> pair<A, B> make_pair(A a, B b) { return pair<A, B>(a, b); }
  template <class T> void swap(T& a, T& b) { T t = move(a); a = move(b); b = move(t); }
}
struct Tracked { int v; Tracked(int x) : v(x) {} Tracked(const Tracked& o) : v(o.v) { printf("copy %d\n", v); } Tracked(Tracked&& o) : v(o.v) { o.v = -1; printf("move %d\n", v); }
  Tracked& operator=(Tracked&& o) { v = o.v; o.v = -2; return *this; } };
template <class T> int take(T&& t) { Tracked x(u::forward<T>(t)); return x.v; }
int main() {
  u::pair<int, double> p = u::make_pair(3, 0.5);
  int a = 1, b = 2;
  u::swap(a, b);
  Tracked t(7), s(8);
  u::swap(t, s);
  int r1 = take(t);
  int r2 = take(u::move(s));
  printf("%d %g %d %d %d %d %d %d\n", p.first, p.second, a, b, t.v, s.v, r1, r2);
  return 0;
}
