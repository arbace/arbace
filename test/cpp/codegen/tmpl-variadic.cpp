// variadic templates: sizeof..., recursion over a pack, a pack expanded into a call and into forwarding references,
// a class template's parameter pack, the non-variadic overload preferred by partial ordering; packs in partial
// specializations (a tuple's recursion, R(Args...)), a pack deduced from a template-id, a value pack of a dependent type
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
template <class... Ts> struct Tuple;
template <> struct Tuple<> { int size() const { return 0; } };
template <class H, class... T> struct Tuple<H, T...> : Tuple<T...> { H head; int size() const { return 1 + Tuple<T...>::size(); } };
template <class... Ts> int arity(const Tuple<Ts...>&) { return sizeof...(Ts); }
template <class Sig> struct Fn;
template <class R, class... A> struct Fn<R(A...)> { static const int n = sizeof...(A); R (*p)(A...); R call(A... a) { return p(a...); } };
int add3(int a, int b, int c) { return a + b + c; }
template <class T, T... V> struct Sum;
template <class T> struct Sum<T> { static const T value = 0; };
template <class T, T v, T... R> struct Sum<T, v, R...> { static const T value = v + Sum<T, R...>::value; };
int main() {
  int i = 7; double d = 0.25; char c = 'q';
  printf("%d %d %d %d\n", Types<int, char, double>::n, Types<>::n, count(1, 2.0, 'c'), count());
  printf("%d %g %d\n", sum(1, 2, 3, 4), sum(0.5, 1, 2), total<1, 2, 3>());
  call(1, 2.5, 'x');
  relay(i, d, c);
  Tuple<int, char, double> t;
  Fn<int(int, int, int)> f; f.p = add3;
  printf("%d %d %d %d %ld\n", t.size(), arity(t), Fn<int(int, int, int)>::n, f.call(1, 2, 3), Sum<long, 1, 2, 3, 4>::value);
  return 0;
}
