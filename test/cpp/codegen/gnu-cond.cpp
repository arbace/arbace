// GNU's conditional with an omitted operand, x ?: y (clang's BinaryConditionalOperator; cpp-route): x evaluated once, a glvalue
// by its address, a class prvalue materialized, a scalar by its value; and [expr.cond]/4-7: a class operand converted to the
// other's type by its conversion function, operands of one type keep it (bool ? bool : bool is a bool)
#include <cstdio>
static int calls;
static int next() { return ++calls; }
static int zero() { ++calls; return 0; }
struct X { int v; X(int v) : v(v) { calls += 10; } operator bool() const { return v != 0; } };
int main() {
  int a = next() ?: 5;
  int b = zero() ?: 7;
  std::printf("%d %d %d\n", a, b, calls);
  int u = 0, w = 3;
  int& r = u ?: w;
  r = 9;
  std::printf("%d %d\n", u, w);
  const char* s = nullptr;
  const char* t = s ?: "fallback";
  std::printf("%s\n", t);
  calls = 0;
  bool c1 = X(1) ?: false;
  bool c2 = X(0) ? X(2) : false;
  std::printf("%d %d %d\n", c1, c2, calls);
  bool f = true, g = false;
  auto h = a > 1 ? f : g;
  std::printf("%d %d\n", (int)sizeof(h), h);
  return 0;
}
