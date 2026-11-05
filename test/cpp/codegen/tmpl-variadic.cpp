// variadic templates: sizeof..., recursion over a pack, a pack expanded into a call and into forwarding references,
// a class template's parameter pack, the non-variadic overload preferred by partial ordering
#include <cstdio>
template <class... Ts> struct Types { static const int n = sizeof...(Ts); };
template <class... Ts> int count(Ts...) { return sizeof...(Ts); }
template <class T> T sum(T t) { return t; }
template <class T, class... R> T sum(T t, R... r) { return t + sum(r...); }
void show(int a, double b, char c) { printf("%d %g %c\n", a, b, c); }
template <class... A> void call(A... a) { show(a...); }
template <class T> T&& fwd(T& t) { return static_cast<T&&>(t); }
template <class... A> void relay(A&&... a) { show(fwd<A>(a)...); }
template <int... N> int total() { return sizeof...(N); }
int main() {
  int i = 7; double d = 0.25; char c = 'q';
  printf("%d %d %d %d\n", Types<int, char, double>::n, Types<>::n, count(1, 2.0, 'c'), count());
  printf("%d %g %d\n", sum(1, 2, 3, 4), sum(0.5, 1, 2), total<1, 2, 3>());
  call(1, 2.5, 'x');
  relay(i, d, c);
  return 0;
}
