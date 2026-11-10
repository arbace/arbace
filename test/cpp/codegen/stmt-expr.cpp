// GNU's statement expressions, ({ s...; e; }) (GCC's manual, "Statements and Declarations in Expressions"; cpp-route): the
// statements in a scope of their own, their locals destroyed at its end, the last expression's value the value; and C++20's
// range-based for with an init-statement ([stmt.ranged]/1), over an array and over a class
#include <cstdio>
#include <vector>
struct D { int v; ~D() { std::printf("~D %d\n", v); } };
int main() {
  int a = ({ int t = 3; t * 2; });
  int b = ({ D d{a}; d.v + 1; });
  ({ std::printf("void %d\n", b); });
  int arr[] = {1, 2, 3};
  int s = 0;
  for (int k = ({ 10; }); auto x : arr) s += x * k;
  std::vector<int> v{4, 5};
  for (int m = 2; int y : v) s += y * m;
  std::printf("%d %d %d\n", a, b, s);
  return s == 78 ? 0 : 1;
}
