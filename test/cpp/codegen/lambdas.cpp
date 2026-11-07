// lambdas: captures by value and by reference, implicit captures [=] and [&], mutable, deduced and explicit return
// types, this captured in a member function, a captureless lambda converted to a function pointer
#include <cstdio>
struct Acc {
  int total = 0;
  void add(int k) { auto f = [this, k](int m) { total += k * m; }; f(1); f(2); }
  int scaled(int s) const { auto g = [=]() { return total * s; }; return g(); }
};
int apply(int (*fn)(int), int v) { return fn(v); }
inline int twiceInline(int a) { auto f = [a](int x) { return x + a; }; auto g = [&a]() { a *= 2; return a; }; return f(g()); }
int main() {
  int a = 3, b = 4;
  auto add = [a](int x) { return x + a; };
  auto bump = [&b]() { b++; };
  bump(); bump();
  printf("%d %d\n", add(10), b);
  auto all = [=]() { return a * b; };
  auto allRef = [&]() { a = 100; b = 200; };
  printf("%d\n", all());
  allRef();
  printf("%d %d %d\n", a, b, all());
  int counter = 0;
  auto next = [counter]() mutable { return ++counter; };
  printf("%d %d %d outer %d\n", next(), next(), next(), counter);
  auto half = [](double d) -> int { return d / 2; };
  printf("%d\n", half(9.0));
  int (*fp)(int) = [](int x) { return x * x; };
  printf("%d %d\n", fp(7), apply([](int x) { return x - 1; }, 5));
  Acc acc; acc.add(5);
  printf("%d %d\n", acc.total, acc.scaled(3));
  printf("%d\n", twiceInline(4));
  printf("%d\n", [](int p, int q) { return p * 10 + q; }(4, 2));
  return 0;
}
