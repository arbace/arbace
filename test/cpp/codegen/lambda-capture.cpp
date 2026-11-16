// lambdas: a captured class object copied (its copy constructor and destructor), a by-reference capture that sees
// later changes, lambdas stored and called in a loop, a lambda returning a class, nested lambdas
#include <cstdio>
struct Tag { int n; Tag(int n) : n(n) {} Tag(const Tag& o) : n(o.n) { printf("copy Tag %d\n", n); } ~Tag() { printf("~Tag %d\n", n); } };
struct Pt { int x, y; };
int main() {
  Tag t(7);
  {
    auto byValue = [t]() { return t.n * 2; };
    auto byRef = [&t]() { return t.n * 3; };
    t.n = 8;
    printf("%d %d\n", byValue(), byRef());
  }
  int base = 5;
  auto mk = [base](int k) { return Pt{base + k, base * k}; };
  for (int i = 0; i < 3; i++) { Pt p = mk(i); printf("(%d,%d) ", p.x, p.y); }
  printf("\n");
  auto outer = [base](int k) { auto inner = [k, base]() { return k * 10 + base; }; return inner() + 1; };
  printf("%d\n", outer(3));
  return 0;
}
