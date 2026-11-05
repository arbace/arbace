#include <cstdio>
auto f1 = [](int x) { return x + 1; };
namespace N { auto f2 = [](int x) { return x * 2; }; }
static int g(int x) { auto l = [](int y) { return y - 1; }; return l(x); }
auto f3 = [](int x) { return x + 10; };
int main() { printf("%d %d %d %d\n", f1(1), N::f2(2), g(3), f3(4)); return 0; }
