// arguments computed by constant expressions: sizeof, arithmetic and comparisons of other parameters, a static constant
#include <cstdio>
template <int N> struct Int { static const int value = N; };
template <class T> struct Bits { static const int value = (int)sizeof(T) * 8; };
template <int A, int B> int add() { return Int<A + B>::value; }
template <class T, int N = (int)sizeof(T) * 2> int doubled() { return N; }
int main() {
  printf("%d %d %d %d\n", Bits<char>::value, Bits<long>::value, add<2, 3>(), Int<(4 > 3) ? 10 : 20>::value);
  printf("%d %d %d\n", doubled<int>(), doubled<double>(), doubled<char, 1>());
  return Int<Bits<short>::value>::value - 16;
}
