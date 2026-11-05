// scopes: shadowing in blocks, loops and conditions, names of the class against the namespace's, the global qualifier
#include <cstdio>
int x = 1;
namespace n { int x = 2; int get() { return x; } }
struct S { int x = 3; int get() const { return x + ::x + n::x; } int outer() const { int x = 10; return x + this->x; } };
int main() {
  int x = 4;
  printf("%d %d\n", x, ::x);
  { int x = 5; printf("%d\n", x); { x++; int x = 7; printf("%d\n", x); } printf("%d\n", x); }
  for (int x = 0; x < 2; x++) printf("loop %d\n", x);
  if (int y = x * 2) printf("cond %d\n", y);
  S s;
  printf("%d %d %d\n", s.get(), s.outer(), n::get());
  return x;
}
