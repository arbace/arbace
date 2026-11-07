// compile-time recursion: a function template and a class template instantiating themselves, ended by specializations
#include <cstdio>
template <int N> int fact() { return N * fact<N - 1>(); }
template <> int fact<0>() { return 1; }
template <int N> struct Fib { static int get() { return Fib<N - 1>::get() + Fib<N - 2>::get(); } };
template <> struct Fib<0> { static int get() { return 0; } };
template <> struct Fib<1> { static int get() { return 1; } };
int main() {
  printf("%d %d %d\n", fact<5>(), fact<1>(), Fib<10>::get());
  return 0;
}
