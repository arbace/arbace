// constant evaluation: constexpr functions (recursion, loops, locals, conditions) folded where clang folds them --
// globals' initializers, constexpr and const integral locals, enumerators and case labels -- and called at run time
// otherwise
#include <cstdio>
constexpr int sq(int x) { return x * x; }
constexpr long fact(int n) { return n <= 1 ? 1 : n * fact(n - 1); }
constexpr int sumTo(int n) { int s = 0; for (int i = 1; i <= n; i++) s += i; return s; }
constexpr unsigned bits(unsigned v) { unsigned c = 0; while (v) { c += v & 1; v >>= 1; } return c; }
constexpr int collatz(int n) { int steps = 0; while (n != 1) { if (n % 2 == 0) n /= 2; else n = 3 * n + 1; ++steps; } return steps; }
enum Sizes { Small = sq(2), Big = sq(5), Count = sumTo(4) };
int g1 = sq(7);
long g2 = fact(12);
const int g3 = sumTo(100);
double g4 = sq(3) / 2.0;
int classify(int v) {
  switch (v) { case sq(2): return 1; case sumTo(3): return 2; default: return 0; }
}
int main() {
  constexpr int a = sumTo(10);
  const int b = bits(0xF0F0u);
  int c = sq(b);
  constexpr long f = fact(15);
  printf("%d %d %d %ld\n", a, b, c, f);
  printf("%d %ld %d %.2f\n", g1, g2, g3, g4);
  printf("%d %d %d\n", Small, Big, Count);
  printf("%d %d %d\n", classify(4), classify(6), classify(5));
  printf("%d %d\n", collatz(27), collatz(c));
  return 0;
}
